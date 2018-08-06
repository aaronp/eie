package eie.dao
import agora.io.FromBytes
import eie.BaseIOSpec

import scala.collection.mutable.ListBuffer

class IdDaoTest extends BaseIOSpec {

  implicit val intFromBytes = FromBytes.Utf8String.map(_.toInt)

  "FileIdDao.save" should {
    "write down things by their id" in {
      withDir { dir =>
        val saved = ListBuffer[(String, Int)]()
        implicit val persist = Persist[Int] {
          case (path, value) =>
            saved += (path.fileName -> value)
        }
        val dao = IdDao[Int](dir)
        dao.save("one", 1)
        saved.toList shouldBe List("one" -> 1)

        dao.save("one", 2)
        saved.toList shouldBe List("one" -> 1, "one" -> 2)
      }
    }
  }

}
