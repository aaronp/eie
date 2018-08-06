package eie.io

class LowPriorityIOImplicitsTest extends BaseIOSpec with LowPriorityIOImplicits {
  "RichPath.text_=" should {
    "truncate existing text" in {
      withDir { dir =>
        dir.resolve("file").text = "first\nsecond\tthird"
        dir.resolve("file").text shouldBe "first\nsecond\tthird"
        dir.resolve("file").text = "fourth"
        dir.resolve("file").text shouldBe "fourth"
      }
    }
  }
  "RichPath.nestedFiles" should {
    "list all nested files" in {
      withDir { dir =>
        dir.resolve("a").resolve("b").resolve("c").text = "see"
        dir.resolve("x").resolve("y").resolve("z").text = "zee"
        dir.resolve("x").resolve("file.txt").text = "file"
        dir.resolve("foo").text = "bar"

        dir.nestedFiles().map(_.fileName).toList should contain only ("c", "z", "file.txt", "foo")
      }
    }
    "list all nested files up to a max depth" in {
      withDir { dir =>
        dir.resolve("a").resolve("b").resolve("c").text = "see"
        dir.resolve("x").resolve("y").resolve("z").text = "zee"
        dir.resolve("x").resolve("file.txt").text = "file"
        dir.resolve("foo").text = "bar"

        dir.nestedFiles(0).map(_.fileName).toList should contain only ("a", "x", "foo")
        dir.nestedFiles(1).map(_.fileName).toList should contain only ("b", "y", "file.txt", "foo")
        dir.nestedFiles(2).map(_.fileName).toList should contain only ("c", "z", "file.txt", "foo")
      }
    }
  }
  "RichPath.createLinkFrom" should {
    "create symbolic links" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "Some file data"
        val link = file.createSymbolicLinkFrom(dir.resolve("file.a"))

        link.text shouldBe "Some file data"
        file.delete()

        // the file linked to is now gone - the link should be empty
        link.text shouldBe ""
      }
    }
    "keep the source file when the link is deleted" in {
      withDir { dir =>
        val file = dir.resolve("file.txt").text = "Some file data"
        val link = file.createSymbolicLinkFrom(dir.resolve("file.link"))
        file.exists() shouldBe true
        link.exists() shouldBe true
        link.delete()
        file.exists() shouldBe true
        link.exists() shouldBe false
      }
    }
  }

}
