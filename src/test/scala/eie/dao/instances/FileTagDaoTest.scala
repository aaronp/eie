package eie.dao.instances
import eie.dao.Persist
import eie.io.{BaseIOSpec, FromBytes}

class FileTagDaoTest extends BaseIOSpec {
  import eie.dao.TagDaoTest.identity
  "FileTagDao.setTag" should {
    "create a ROOT/tags/*tag*/*value*/*id*/.data entry for alphanumeric tag values" in {
      withDao { dao =>
        dao.setTag("entry", "some tag", "someValue")

        val entityDir =
          dao.rootDir.resolve("tags").resolve("some tag").resolve("someValue").resolve("entry")
        entityDir.resolve(".data").exists() shouldBe true
        entityDir.resolve(".data").text shouldBe "entry"
        entityDir.resolve(".value").exists() shouldBe false
      }
    }
    "create a ROOT/tags/*tag*/*value*/*id*/.value entry for non-alphanumeric tag values" in {
      withDao { dao =>
        val value = "`weird` non-alphanumeric * value"
        dao.setTag("first", "TAG", value)

        val idDir = dao.rootDir
          .resolve("tags")
          .resolve("TAG")
          .resolve(value.hashCode.toString)
          .resolve("first")
        idDir.resolve(".value").text shouldBe value
        idDir.resolve(".data").text shouldBe "first"
      }
    }
    "create a ROOT/tags/*tag*/*value hash*/*id*/.value entries which hash to the same key" in {
      withDaoHavingMaxValueLen(1) { dao =>
        // https://stackoverflow.com/questions/12925988/how-to-generate-strings-that-share-the-same-hashcode-in-java
        val value1 = "Aa"
        val value2 = "BB"
        withClue("precondition failed: we want to test different tag values which have the same hashCode") {
          value1.hashCode shouldBe value2.hashCode
        }
        dao.setTag("entry1", "sameTag", value1)
        dao.setTag("entry2", "sameTag", value2)

        val valueDir =
          dao.rootDir.resolve("tags").resolve("sameTag").resolve(value1.hashCode.toString)
        val ids = valueDir.children.map(_.fileName).toList
        ids should contain only ("entry1", "entry2")
        valueDir.resolve("entry1").resolve(".value").text shouldBe value1
        valueDir.resolve("entry1").resolve(".data").text shouldBe "entry1"

        valueDir.resolve("entry2").resolve(".value").text shouldBe value2
        valueDir.resolve("entry2").resolve(".data").text shouldBe "entry2"
      }
    }
    "create a ROOT/ids/*id*/*tag* = tag value entry for alphanumeric tag values" in {
      withDao { dao =>
        dao.setTag("content", "Another tag", "foo")

        val tagFile = dao.rootDir.resolve("ids").resolve("content").resolve("Another tag")
        tagFile.text shouldBe "foo"
      }
    }
    "remove the old value when a tag value is updated" in {
      withDao { dao =>
        dao.valueForTag("first", "TAG") shouldBe empty

        dao.setTag("first", "TAG", "firstvalue")
        dao.rootDir.resolve("tags").resolve("TAG").resolve("firstvalue").exists() shouldBe true

        dao.valueForTag("first", "TAG") shouldBe Some("firstvalue")

        // now update the tag
        dao.setTag("first", "TAG", "updatedValue")
        dao.valueForTag("first", "TAG") shouldBe Some("updatedValue")

        dao.rootDir.resolve("tags").resolve("TAG").resolve("firstvalue").exists() shouldBe false
        dao.rootDir.resolve("tags").resolve("TAG").resolve("updatedValue").exists() shouldBe true
      }
    }
    "keep the old value but remote the id when a removed tag value is still referred to by a different entry" in {
      withDao { dao =>
        dao.setTag("first", "TAG", "first value")
        dao.setTag("second", "TAG", "first value")

        val firstValueDir =
          dao.rootDir.resolve("tags").resolve("TAG").resolve("first value".hashCode.toString)
        firstValueDir.exists() shouldBe true
        firstValueDir.resolve("first").exists() shouldBe true
        firstValueDir.resolve("second").exists() shouldBe true

        dao.setTag("first", "TAG", "updated value")
        firstValueDir.resolve("first").exists() shouldBe false
        firstValueDir.resolve("second").exists() shouldBe true

        dao.rootDir
          .resolve("tags")
          .resolve("TAG")
          .resolve("updated value".hashCode.toString)
          .resolve("first")
          .exists() shouldBe true
      }
    }
  }

  implicit val fromBytes = FromBytes.Utf8String
  implicit val persist   = Persist.writer[String]
  def withDao[T](f: FileTagDao[String] => T): T = {
    withDir { dir =>
      f(FileTagDao[String](dir, 20, 20))
    }
  }

  def withDaoHavingMaxValueLen[T](maxValueLen: Int)(f: FileTagDao[String] => T): T = {
    withDir { dir =>
      f(FileTagDao[String](dir, 20, maxValueLen))
    }
  }

}
