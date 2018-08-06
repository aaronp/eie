package eie.dao
import agora.io.FromBytes
import eie.BaseIOSpec
import eie.dao.TagDao
import eie.dao.instances.FileTagDao

import scala.util.Try

object TagDaoTest {

  implicit case object identity extends HasId[String] {
    override def id(value: String) = value
  }

  def withDao[T](f: TagDao[String] => T) = {
    BaseIOSpec.withDir("TagDaoTest") { dir =>
      implicit val persist = Persist.writer[String]
      implicit val reader = FromBytes.lift { bytes =>
        val tri = Try(new String(bytes))
        tri
      }

      val dao: FileTagDao[String] = TagDao[String](dir)

      f(dao)
    }
  }
}

class TagDaoTest extends BaseIOSpec {

  import TagDaoTest._

  "TagDao.setTag" should {
    "be able to read back the tags it sets" in {
      withDao { dao =>
        verifyEmpty(dao, "first", "hello")
        verifyEmpty(dao, "second", "hello")

        // call the method under test - set 'hello -> world' on entry 'first'
        dao.setTag("first", "hello", "world")

        verifyTag(dao, "first", "hello", "world")
        verifyEmpty(dao, "second", "hello")
      }
    }
    "be able to set tags with special characters in the values" in {
      withDao { dao =>
        val value = "S@m3 Sp€ci±l C§hars"
        verifyEmpty(dao, "foo", "special tag")

        // call the method under test - set 'hello -> world' on entry 'first'
        dao.setTag("foo", "special tag", value)

        verifyTag(dao, "foo", "special tag", value)
      }
    }
  }
  "TagDao.removeTag" should {
    "remove tags from entries" in {
      withDao { dao =>
        verifyEmpty(dao, "data", "tag 1")
        verifyEmpty(dao, "data", "tag 2")

        // add one tag
        dao.setTag("data", "tag 1", "value 1")
        verifyTag(dao, "data", "tag 1", "value 1")

        // add another
        dao.setTag("data", "tag 2", "value 2")
        verifyTag(dao, "data", "tag 2", "value 2")

        // call the method under test - remove the first
        dao.removeTag("data", "tag 1")

        verifyEmpty(dao, "data", "tag 1")
        verifyTag(dao, "data", "tag 2", "value 2")
      }
    }
  }
  "TagDao.remove" should {
    "have no effect for unknown tags" in {
      withDao { dao =>
        dao.remove("unknown")

        verifyEmpty(dao, "unknown", "foo")
      }
    }
  }

  def verifyEmpty(dao: TagDao[String], data: String, tag: String) = {
    dao.tagsFor(data) should not contain (tag)
    dao.hasTag(data, tag) shouldBe false
    dao.valueForTag(data, tag) shouldBe empty
    dao.findDataWithTag(tag).toList should not contain (data)
  }

  def verifyTag(dao: TagDao[String], data: String, tag: String, value: String) = {
    dao.findDataWithTag(tag).toList should contain(data)
    dao.findDataWithTagValue(tag, value).toList should contain(data)

    dao.tagsFor(data).toList should contain(tag)
    dao.hasTag(data, tag) shouldBe true
    dao.valueForTag(data, tag) shouldBe Option(value)
  }
}
