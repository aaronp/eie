package eie.dao.instances

import java.time.{LocalDateTime, ZoneOffset}

import agora.io.FromBytes
import agora.io.dao.HasId
import eie.BaseIOSpec
import eie.dao.instances.FileTimestampDao

class FileTimestampDaoTest extends BaseIOSpec {

  "FileTimestampDao.save" should {
    "write down entries against thei timestamp" in {
      withDir { dir =>
        implicit val p = Persist.save[String]()
        import HasId.implicits._
        implicit val fromBytes = FromBytes.Utf8String

        val dao = FileTimestampDao[String](dir)
        dao.firstId shouldBe None
        val timestamp = LocalDateTime.now(ZoneOffset.UTC).atZone(ZoneOffset.UTC)

        // call the method under test
        val test = dao.save("test", timestamp)

        test.text shouldBe "test"

        dao.find(timestamp, timestamp).toList should contain only ("test")
        dao.find(timestamp.minusNanos(2), timestamp.minusNanos(1)).toList shouldBe empty
        dao.find(timestamp.plusNanos(1), timestamp.plusNanos(2)).toList shouldBe empty
      }
    }
  }
}
