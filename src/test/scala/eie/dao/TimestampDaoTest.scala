package eie.dao

import java.nio.file.Path
import java.time.{LocalDateTime, ZoneId}

import eie.io.{BaseIOSpec, FromBytes, ToBytes}

class TimestampDaoTest extends BaseIOSpec {

  import TimestampDaoTest._

  val Jan2 = LocalDateTime.of(2000, 1, 20, 3, 40, 50, 6789).atZone(ZoneId.systemDefault())

  case class Expect(time: Timestamp, from: Timestamp, to: Timestamp, findMe: Boolean) {
    def dao(dir: Path) = TimestampDao[Value](dir)(persist, valueFromBytes, ValueId)

    def insert(dir: Path): Value = {
      val value = Value("first", s"entry for $time")
      dao(dir).save(value, time)

      withClue("a single entry should result in a timestamped file") {
        dir.nestedFiles().toList.size shouldBe 1
      }
      value
    }
  }

  def beforeTimes = List(
    Expect(Jan2, Jan2.minusNanos(2), Jan2.minusNanos(1), false),
    Expect(Jan2, Jan2.minusSeconds(2), Jan2.minusSeconds(1), false),
    Expect(Jan2, Jan2.minusMinutes(2), Jan2.minusMinutes(1), false),
    Expect(Jan2, Jan2.minusHours(2), Jan2.minusHours(1), false),
    Expect(Jan2, Jan2.minusDays(2), Jan2.minusDays(1), false),
    Expect(Jan2, Jan2.minusMonths(2), Jan2.minusMonths(1), false),
    Expect(Jan2, Jan2.minusYears(2), Jan2.minusYears(1), false)
  )

  def afterTimes = List(
    Expect(Jan2, Jan2.plusNanos(2), Jan2.plusNanos(3), false),
    Expect(Jan2, Jan2.plusSeconds(2), Jan2.plusSeconds(3), false),
    Expect(Jan2, Jan2.plusMinutes(2), Jan2.plusMinutes(3), false),
    Expect(Jan2, Jan2.plusHours(2), Jan2.plusHours(3), false),
    Expect(Jan2, Jan2.plusDays(2), Jan2.plusDays(3), false),
    Expect(Jan2, Jan2.plusMonths(2), Jan2.plusMonths(3), false),
    Expect(Jan2, Jan2.plusYears(2), Jan2.plusYears(3), false)
  )

  def inTimes = {
    for {
      before <- beforeTimes.map(_.from)
      after  <- afterTimes.map(_.to)
    } yield {
      Expect(Jan2, before, after, true)
    }
  }

  def times = afterTimes ++ beforeTimes ++ inTimes

  "TimestampDao.remove" should {
    "remove empty directories after the entry is removed" in {
      withDir { dir =>
        val dao = TimestampDao[Value](dir)

        def dirs = {
          val paths = dir.nestedFiles().map { file =>
            file.parents.map(_.fileName).take(3).reverse.mkString("", "/", s"/${file.fileName}")
          }
          paths.toList.sorted
        }

        // create entries which will end up w/ different nanos, seconds, minutes, hours, and dates
        dao.save(Value("a", "a"), Jan2)
        dao.save(Value("b", "b"), Jan2.plusNanos(1))
        dao.save(Value("c", "c"), Jan2.plusSeconds(1))
        dao.save(Value("d", "d"), Jan2.plusMinutes(1))
        dao.save(Value("e", "e"), Jan2.plusHours(1))
        dao.save(Value("f", "f"), Jan2.plusDays(1))

        // check precondition - we should now have this:
        dirs shouldBe List(
          "2000-1-20/3/40/50_6789_a",
          "2000-1-20/3/40/50_6790_b",
          "2000-1-20/3/40/51_6789_c",
          "2000-1-20/3/41/50_6789_d",
          "2000-1-20/4/40/50_6789_e",
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("a", "a"), Jan2)
        dirs shouldBe List(
          "2000-1-20/3/40/50_6790_b",
          "2000-1-20/3/40/51_6789_c",
          "2000-1-20/3/41/50_6789_d",
          "2000-1-20/4/40/50_6789_e",
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("b", "b"), Jan2.plusNanos(1))
        dirs shouldBe List(
          "2000-1-20/3/40/51_6789_c",
          "2000-1-20/3/41/50_6789_d",
          "2000-1-20/4/40/50_6789_e",
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("c", "c"), Jan2.plusSeconds(1))
        dirs shouldBe List(
          "2000-1-20/3/41/50_6789_d",
          "2000-1-20/4/40/50_6789_e",
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("d", "d"), Jan2.plusMinutes(1))
        dirs shouldBe List(
          "2000-1-20/4/40/50_6789_e",
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("e", "e"), Jan2.plusHours(1))
        dirs shouldBe List(
          "2000-1-21/3/40/50_6789_f"
        )

        dao.remove(Value("f", "f"), Jan2.plusDays(1))
        dirs shouldBe empty

      }
    }
  }
  "TimestampDao.find" should {
    times.foreach {
      case e @ Expect(time, from, to, true) =>
        s"find $time given $from - $to" in {
          withDir { dir =>
            e.dao(dir).first shouldBe empty
            e.dao(dir).last shouldBe empty

            val value = e.insert(dir)

            e.dao(dir).find(from, to).toList should contain(value)

            e.dao(dir).first.map(_.toLocalDateTime()) shouldBe Option(time.toLocalDateTime)
            e.dao(dir).last.map(_.toLocalDateTime()) shouldBe Option(time.toLocalDateTime)

            // save another slightly later
            val nextValue = Value("second", s"entry for $time")
            e.dao(dir).save(nextValue, time.plusNanos(1))

            e.dao(dir).first.map(_.toLocalDateTime()) shouldBe Option(time.toLocalDateTime)
            e.dao(dir).last.map(_.toLocalDateTime()) shouldBe Option(time.plusNanos(1).toLocalDateTime)
          }
        }
      case e @ Expect(time, from, to, false) =>
        s"not find $time given $from - $to" in {
          withDir { dir =>
            val value = e.insert(dir)
            e.dao(dir).find(from, to).toList should not contain (value)
          }
        }
    }

    "find saved values within a time range of the specific timestamp" in {
      withDir { dir =>
        val dao = TimestampDao[Value](dir)

        val value = Value("first", "alpha")
        dao.save(value, Jan2)

        withClue("a single entry should result in a timestamped file") {
          dir.nestedFiles().toList.size shouldBe 1
        }

        dao.find(Jan2, Jan2).toList should contain(value)
      }

    }
  }
}

object TimestampDaoTest {

  case class Value(id: String, name: String)

  implicit val valueToBytes = ToBytes.Utf8String.contramap[Value] { value =>
    s"${value.id};${value.name}"
  }
  implicit val valueFromBytes: FromBytes[Value] = FromBytes.Utf8String.map { str =>
    val List(id, name) = str.split(";", -1).toList
    Value(id, name)
  }

  implicit val persist: Persist[Value] = Persist.writer[Value]

  implicit object ValueId extends HasId[Value] {
    override def id(value: Value): String = value.id
  }

}
