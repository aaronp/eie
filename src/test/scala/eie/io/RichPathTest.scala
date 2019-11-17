package eie.io

import java.nio.file.attribute.PosixFilePermission
import java.time.{ZoneId, ZonedDateTime}

class RichPathTest extends BaseIOSpec {

  "RichPath.size" should {
    "return the file size" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "foo"
        file.size shouldBe "foo".getBytes.size
      }
    }
  }
  "RichPath.deleteAfter" should {
    "delete the file after the thunk completes" in {
      val testDir = s"./target/${getClass.getSimpleName}${System.currentTimeMillis()}".asPath.mkDirs().deleteAfter { dir =>
        dir
      }
      testDir.exists() shouldBe false
    }
  }
  "RichPath.grantAllPermissions" should {
    "set all the file permissions" in {
      withDir { dir =>
        val file = dir.resolve("allAccess").createIfNotExists().grantAllPermissions
        file.grantAllPermissions.permissions() should contain theSameElementsAs (PosixFilePermission.values())
      }
    }
  }
  "RichPath.append" should {
    "append content to a file" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").createIfNotExists().append("hello ").append("world")
        file.text shouldBe "hello world"
      }
    }
  }
  "RichPath.createSymbolicLinkTo" should {
    "create symbolic links" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "content"
        val link = dir.resolve("imA.link").createSymbolicLinkTo(file)
        link.text shouldBe "content"
      }
    }
  }
  "RichPath.createHardLinkFrom" should {
    "create hard links" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "content"
        val link = file.createHardLinkFrom(dir.resolve("imA.link"))
        link.text shouldBe "content"
      }
    }
  }
  "RichPath.bytes_=" should {
    "set the contents of the file to the bytes" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").bytes = Array(1, 2, 3)
        file.bytes shouldBe Array[Byte](1, 2, 3)
      }
    }
  }
  "RichPath.createdUTC" should {
    "return the created timestamp of the file" in {
      withDir { dir =>
        val before = ZonedDateTime.now(ZoneId.of("UTC"))
        Thread.sleep(1500)
        val file = dir.resolve("file.txt").createIfNotExists()
        Thread.sleep(1500)
        val after = ZonedDateTime.now(ZoneId.of("UTC"))
        withClue(s"${file.createdUTC} shouldBe between $before and $after") {
          file.createdUTC.isAfter(after) shouldBe false
          file.createdUTC.isBefore(before) shouldBe false
        }
      }
    }
  }
  "RichPath.renderTree" should {
    "show the file paths as a tree" in {
      withDir { dir =>
        dir.resolve("test/a/b1/c1/fileA.txt").text = "hi"
        dir.resolve("test/a/b1/c2/fileB.txt").text = "hi"
        dir.resolve("test/a/b2/foo.txt").text = "hi"

        def trim(text: String) = {
          val padding = text.linesIterator.map(_.takeWhile(_.isWhitespace).size).min
          text.linesIterator.map(_.drop(padding)).mkString(System.lineSeparator())
        }

        val expected = """ test
                         >      +- a
                         >         +- b1
                         >         |    +- c1
                         >         |        +- fileA.txt
                         >         |    +- c2
                         >         |        +- fileB.txt
                         >         +- b2
                         >             +- foo.txt""".stripMargin('>')
        trim(dir.resolve("test").renderTree()) shouldBe trim(expected)


        val actual = dir.resolve("test").renderTree(f => f.isDir || f.fileName == "fileA.txt")
        val filteredExpected = """ test
                                 >      +- a
                                 >         +- b1
                                 >         |    +- c1
                                 >         |        +- fileA.txt
                                 >         |    +- c2
                                 >         +- b2""".stripMargin('>')
        trim(actual) shouldBe trim(filteredExpected)
      }
    }
  }
  "RichPath.setFilePermissions" should {
    "set the file permissions" in {
      withDir { dir =>
        val file = dir.resolve("allAccess").createIfNotExists().grantAllPermissions

        import PosixFilePermission._
        file.setFilePermissions(OTHERS_WRITE, OWNER_EXECUTE).permissions() should contain only (OTHERS_WRITE, OWNER_EXECUTE)
      }
    }
  }
  "RichPath.search" should {
    "find nested files" in {
      withDir { dir =>
        val a = dir.resolve("abc/b/c/file.txt").text = "contents"
        val b = dir.resolve("cde/f/g.txt").text = "meh"
        dir.search(10)(_.fileName == "file.txt").toList should contain only (a)
        dir.search(2)(_.fileName == "file.txt").toList shouldBe empty
      }
    }
  }
  "RichPath.childrenMatchingName" should {
    "return the children matching the predicate" in {
      withDir { dir =>
        dir.resolve("abc/b/c.file.txt").text = "contents"
        dir.resolve("cde/f/g.txt").text = "meh"
        dir.childrenMatchingName(_.contains("c")).map(_.fileName) should contain only ("abc", "cde")
        dir.childrenMatchingName(_ == "abc").map(_.fileName) should contain only ("abc")
        val file = dir.resolve("meh.txt").text = "this is a file"
        file.childrenMatchingName(_ => true) shouldBe empty
        dir
      }
    }
  }
}
