package eie.io

import java.nio.file.attribute.PosixFilePermission
import java.time.{ZoneId, ZonedDateTime}
import org.scalactic.Prettifier.default
import org.scalatest.matchers.should.Matchers

object RichPathTest extends Matchers {

  def trim(text: String): String = {
    val padding = text.linesIterator.map(_.takeWhile(_.isWhitespace).size).min.toInt
    def sanitise(x: String) =
      x.drop(padding)
        .replace("😃", "|")
        .replace(" ", ".")

    text.linesIterator.map(sanitise).mkString(System.lineSeparator())
  }

  def check(actualFull: String, expectedFull: String) = {
    val actual   = trim(actualFull)
    val expected = trim(expectedFull)
    actual.linesIterator.size shouldBe expected.linesIterator.size
    actual.linesIterator.zip(expected.linesIterator).foreach {
      case (a, e) => a shouldBe e
    }
  }

}

class RichPathTest extends BaseIOSpec {

  import RichPathTest._

  "RichPath.size" should {
    "return the file size" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "foo"
        file.size shouldBe "foo".getBytes.size
      }
    }
  }
  "RichPath.inputStream" should {
    "open a file input stream" in {
      withDir { dir =>
        val is = (dir.resolve("file.txt").text = "hi\nthere").inputStream()
        scala.io.Source.fromInputStream(is).getLines().toList shouldBe List("hi", "there")
      }
    }
  }
  "RichPath.text" should {
    "get and set file contents" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = """hello
            |world
            |123""".stripMargin
        file.text shouldBe """hello
                             |world
                             |123""".stripMargin
        file.lines.toList shouldBe List("hello", "world", "123")
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
          file.lastModifiedMillis should be >= before.toInstant.toEpochMilli
          file.lastModifiedMillis should be <= after.toInstant.toEpochMilli
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

        val expected: String = """test
                         |     +- a
                         |        +- b1
                         |        😃    +- c1
                         |        😃        +- fileA.txt
                         |        😃    +- c2
                         |        😃        +- fileB.txt
                         |        +- b2
                         |            +- foo.txt""".stripMargin

        check(dir.resolve("test").renderTree(), expected)

        val actual                   = dir.resolve("test").renderTree(f => f.isDir || f.fileName == "fileA.txt")
        val filteredExpected: String = """test
                                 |     +- a
                                 |        +- b1
                                 |        😃    +- c1
                                 |        😃        +- fileA.txt
                                 |        😃    +- c2
                                 |        +- b2""".stripMargin
        check(actual, filteredExpected)
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
  "RichPath.findFirst" should {
    "find nested files" in {
      withDir { dir =>
        val a = dir.resolve("abc/b/c/file1.txt").text = "contents"
        dir.resolve("abc/b/c/d/e/file.txt").text = "contents"
        dir.findFirst(10)(_.fileName == "file1.txt") shouldBe Some(a)
        a.parents.take(3).map(_.fileName) should contain inOrder ("c", "b", "abc")
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
  "RichPath.moveTo" should {
    "move files to a directory" in {
      withDir { dir =>
        val file        = dir.resolve("abc/b/c.file.txt").text = "contents"
        val moved       = file.moveTo(dir)
        val List(check) = dir.nestedFiles(10).toList
        check.parent shouldBe Some(dir)
      }
    }
    "move files to a new file location" in {
      withDir { dir =>
        val file  = dir.resolve("abc/b/c.file.txt").text = "contents"
        val moved = file.moveTo(dir.resolve("also.renamed"))
        moved.fileName shouldBe "also.renamed"
        val List(check) = dir.nestedFiles(10).toList
        check.parent shouldBe Some(dir)
        check.fileName shouldBe "also.renamed"
      }
    }
  }
}
