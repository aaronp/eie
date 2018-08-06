package eie.dao.instances

import java.nio.file.Path
import java.time.{LocalDate, ZoneId, ZoneOffset}

import eie._
import eie.dao.{HasId, Persist, TimeRange, Timestamp, TimestampDao}
import eie.implicits._

import scala.util.Try

/**
  *
  * Writes stuff (T values) down under the given directory structures:
  *
  * <dir>/dates/<date>/<hour>/<minute>/<second>_<nano>_<id> = *data*
  *
  * where *data* is determined by the given [[Persist]], which is either the data itself
  * or a linked file.
  *
  * The implicit Persist may only link to the former instead of serializing the data
  *
  * @param rootDir   the directory under which all the data will be stored
  * @param saveValue the Persist instance to use in saving (or linking) the data.
  * @param fromBytes a means of deserializing a T from bytes
  * @param idFor     the ability to name the T values w/ an ID (as multiple Ts can exist for the same instant)
  * @tparam T the value to save/retrieve
  */
case class FileTimestampDao[T](rootDir: Path)(implicit saveValue: Persist[T], fromBytes: FromBytes[T], idFor: HasId[T]) extends TimestampDao[T] {
  def removeBefore(timestamp: Timestamp) = {
    dateDirs.flatMap(_.removeEntriesBefore(timestamp))
  }

  type Id = String

  type SaveResult   = Path
  type RemoveResult = Path

  import FileTimestampDao._

  private lazy val dateRootDir = rootDir.mkDirs()

  override def save(data: T, timestamp: Timestamp) = {
    val file = timestampedFileForData(data, timestamp)
    saveValue.write(file, data)
    file
  }

  private def timestampedFileForData(data: T, timestamp: Timestamp): Path = {
    val id        = idFor.id(data)
    val fileName  = s"${timestamp.getSecond}_${timestamp.getNano}_$id"
    val minuteDir = resolveDir(timestamp)
    minuteDir.resolve(fileName)
  }

  private def resolveDir(referenceTimestamp: Timestamp): Path = {
    val timestamp = referenceTimestamp.withZoneSameLocal(ZoneId.of("UTC"))
    dateRootDir
      .resolve(dateDir(timestamp))
      .resolve(timestamp.getHour.toString)
      .resolve(timestamp.getMinute.toString)
  }

  private def dateDir(timestamp: Timestamp): String = {
    s"${timestamp.getYear}-${timestamp.getMonthValue}-${timestamp.getDayOfMonth}"
  }

  override def remove(data: T, timestamp: Timestamp) = {
    val deletedFile = timestampedFileForData(data, timestamp).delete()
    tryToRemoveParents(deletedFile)
    deletedFile
  }

  private def read(file: Path): Option[T] = {
    if (file.exists()) {
      fromBytes.read(file.bytes).toOption
    } else {
      None
    }
  }

  private def firstEntry = dateDirs.sorted.flatMap(_.first).headOption

  override def first: Option[Timestamp] = firstEntry.map(_.timestamp)

  def firstId = firstEntry.map(_.id)

  private def lastEntry = dateDirs.sorted.reverse.flatMap(_.last).headOption

  override def last = lastEntry.map(_.timestamp)

  def lastId = lastEntry.map(_.id)

  /**
    * With the directory structure:
    *
    * <date>/<hr>/<min>/<second>_<nanos>_<id>
    *
    * we can know to include *all* the subdirectories or if we have to filter them
    *
    * @param range
    * @return
    */
  override def find(range: TimeRange): Iterator[T] = {
    findEntries(range).map(_.file).flatMap(read)
  }

  final def find(from: Timestamp, to: Timestamp): Iterator[T] = find(TimeRange(from, to))

  def findIds(range: TimeRange): Iterator[String] = findEntries(range).map(_.id)

  private def findEntries(range: TimeRange) = dateDirs.iterator.flatMap(_.inRange(range))

  private def dateDirs: Array[DateDir] = {
    dateRootDir.children.collect {
      case DateDir(dateDir) => dateDir
    }
  }

}

object FileTimestampDao {

  // second_nanosecond_id
  private val TimeFileNameR =
    """(\d+)_(\d+)_(.*)""".r

  /**
    * A saved file is in the form <seconds>_<nanos>_<id>
    */
  object SavedFile {
    def unapply(file: Path): Option[(Int, Int, String, Path)] = {
      file.fileName match {
        case TimeFileNameR(second, nanos, id) => Option((second.toInt, nanos.toInt, id, file))
        case _                                => None
      }
    }
  }

  private object IntDir {
    def unapply(dir: Path): Option[Int] = {
      Try(dir.fileName.toInt).toOption
    }
  }

  private object DateDir {
    private val YearMonthDayR = """(\d\d\d\d)-(\d{1,2})-(\d{1,2})""".r

    def unapply(dir: Path): Option[DateDir] = {
      dir.fileName match {
        case YearMonthDayR(y, m, d) =>
          Option(DateDir(LocalDate.of(y.toInt, m.toInt, d.toInt), dir))
        case _ => None
      }
    }
  }

  /**
    * Represents a 'date' (year-month-date) directory
    */
  private case class DateDir(date: LocalDate, dateDir: Path) extends Ordered[DateDir] {
    def removeEntriesBefore(reference: Timestamp): List[Path] = {
      if (date.isAfter(reference.toLocalDate)) {
        Nil
      } else {
        first.toList.flatMap {
          case StampedFile(firstDate, _, _) =>
            val range = TimeRange(firstDate, reference.minusNanos(1))
            inRange(range).map {
              case StampedFile(ts, _, file) =>
                require(ts.isBefore(reference))
                tryToRemoveParents(file.delete())
            }
        }
      }
    }

    def first: Option[StampedFile] = hours.sorted.flatMap(_.first).headOption

    def last: Option[StampedFile] = hours.sorted.reverse.flatMap(_.last).headOption

    def inRange(range: TimeRange): Iterator[StampedFile] = {
      hours.iterator.flatMap {
        case hourDir if range.completelyContainsDateAndHour(date, hourDir.hour) =>
          hourDir.hourDir.nestedFiles().collect {
            case StampedFile(sf) => sf
          }
        case hourDir => hourDir.inRange(range)
      }
    }

    def hours = dateDir.children.collect {
      case dir @ IntDir(hour) => HourDir(date, hour, dir)
    }

    override def compare(that: DateDir): Int = date.compareTo(that.date)
  }

  private case class HourDir(date: LocalDate, hour: Int, hourDir: Path) extends Ordered[HourDir] {
    def dateAndHour = date.atTime(hour, 0)

    def first: Option[StampedFile] = minutes.sorted.flatMap(_.first).headOption

    def last: Option[StampedFile] = minutes.sorted.reverse.flatMap(_.last).headOption

    def inRange(range: TimeRange): Iterator[StampedFile] = {
      minutes.iterator.flatMap {
        case minuteDir if range.completelyContainsDateHourAndMinute(date, hour, minuteDir.minute) =>
          minuteDir.minuteDir.nestedFiles().collect {
            case StampedFile(sf) => sf
          }
        case minuteDir => minuteDir.inRange(range)
      }
    }

    def minutes = hourDir.children.collect {
      case dir @ IntDir(minute) => MinuteDir(date, hour, minute, dir)
    }

    override def compare(that: HourDir): Int = dateAndHour.compareTo(that.dateAndHour)
  }

  private case class MinuteDir(date: LocalDate, hour: Int, minute: Int, minuteDir: Path) extends Ordered[MinuteDir] {
    def dateAndTime = date.atTime(hour, minute)

    def first: Option[StampedFile] = files.sortBy(_.timestamp).headOption

    def last: Option[StampedFile] = files.sortBy(_.timestamp).lastOption

    def inRange(range: TimeRange) = files.filter(file => range.contains(file.timestamp)).iterator

    def files: Array[StampedFile] = {
      minuteDir.children.collect {
        case SavedFile(second, nano, id, file) =>
          val timestamp = date.atTime(hour, minute, second, nano).atZone(ZoneOffset.UTC)
          StampedFile(timestamp, id, file)
      }
    }

    override def compare(that: MinuteDir): Int = dateAndTime.compareTo(that.dateAndTime)
  }

  private case class StampedFile(timestamp: Timestamp, id: String, file: Path)

  private object StampedFile {
    def unapply(file: Path): Option[StampedFile] = {
      if (file.isFile) {
        file match {
          case SavedFile(second, nano, id, file) =>
            for {
              minuteDir @ IntDir(minute) <- file.parent
              hourDir @ IntDir(hour)     <- minuteDir.parent
              DateDir(dateDir)           <- hourDir.parent
            } yield {
              val timestamp = dateDir.date.atTime(hour, minute, second, nano).atZone(ZoneOffset.UTC)
              StampedFile(timestamp, id, file)
            }
          case _ => None
        }
      } else {
        None
      }
    }
  }

  private def tryToRemoveParents(deletedFile: Path) = {
    // traverse up to the minute, hour and date dirs to see if we can remove 'em
    deletedFile.parents.take(3).filter { parent =>
      if (parent.isEmptyDir) {
        parent.delete()
        true
      } else {
        false
      }
    }
    deletedFile
  }
}
