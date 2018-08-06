package eie.dao.instances

import java.time.{LocalDateTime, ZoneOffset}

import eie.dao.{HasId, Persist}
import eie.io.{BaseIOSpec, FromBytes}

class FileTimestampDaoTest extends BaseIOSpec {

  "FileTimestampDao.save" should {
    "write down entries against the timestamp" in {
      withDir { dir =>
        implicit val p         = Persist.save[String]()
        implicit val strId     = HasId.lift[String](identity)
        implicit val fromBytes = FromBytes.Utf8String

        val dao = FileTimestampDao[String](dir)
        dao.firstId shouldBe None
        dao.first shouldBe None
        dao.last shouldBe None
        val timestamp = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)

        // call the method under test
        val test = dao.save("test", timestamp)

        test.text shouldBe "test"

        dao.first shouldBe Some(timestamp)
        dao.last shouldBe Some(timestamp)

        dao.find(timestamp, timestamp).toList should contain only ("test")
        dao.find(timestamp.minusNanos(2), timestamp.minusNanos(1)).toList shouldBe empty
        dao.find(timestamp.plusNanos(1), timestamp.plusNanos(2)).toList shouldBe empty

      }
    }
  }
}
