package eie.dao

import java.nio.file.Path
import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}

import eie.FromBytes
import eie.dao.instances.FileTimestampDao

/**
  * Provides a means of finding values based on a time range, as well as determining the minimum/maximum time.
  *
  * @tparam T
  */
trait TimestampReader[T] {

  /**
    * Reads back
    *
    * @param inRange
    * @return the entities in the given range, exclusive
    */
  def find(inRange: TimeRange): Iterator[T]

  /** @return The first timestamp stored, if any
    */
  def first(): Option[Timestamp]

  /** @return The last timestamp stored, if any
    */
  def last(): Option[Timestamp]
}

trait TimestampWriter[T] {
  type SaveResult
  type RemoveResult

  def save(data: T, timestamp: Timestamp = TimestampDao.now()): SaveResult

  def remove(data: T, timestamp: Timestamp): RemoveResult
}

trait TimestampDao[T] extends TimestampWriter[T] with TimestampReader[T]

object TimestampDao {

  /**
    * The file instance will store data in the following way:
    *
    * <dir>/<year>/<month><date>/<hour>/<minute>/<second_nano_id> = bytes
    *
    * and
    *
    * <dir>/<ids>/<id> = <timestamp>
    *
    * @param dir
    * @tparam T
    * @return
    */
  def apply[T](dir: Path)(implicit saveValue: Persist[T], fromBytes: FromBytes[T], idFor: HasId[T]): FileTimestampDao[T] = {
    new FileTimestampDao(dir)
  }

  def now(zone: ZoneOffset = ZoneOffset.UTC) = ZonedDateTime.now(zone)
  def fromEpochNanos(epochNanos: Long, zone: ZoneOffset = ZoneOffset.UTC): Timestamp = {
    val second = epochNanos / 1000000
    val nanos  = (epochNanos % 1000000).toInt
    LocalDateTime.ofEpochSecond(second, nanos, zone).atZone(zone)
  }

}
