package eie.io

class PathTreeNodeTest extends BaseIOSpec {

  import RichPathTest._

  "path.renderTree" should {
    "render the path as a tree" in {
      withDir { dir =>
        val path = dir.resolve("root")
        path.resolve("childA").resolve("grandChildA").resolve("A").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("B").resolve("B1").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("B").resolve("B2").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("C").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("X").resolve("meh").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("X").resolve("foo1").createIfNotExists()
        path.resolve("childA").resolve("grandChildA").resolve("X").resolve("foo2").createIfNotExists()

        path.resolve("childA").resolve("grandChildB").resolve("A").createIfNotExists()
        path.resolve("childA").resolve("grandChildB").resolve("B").createIfNotExists()
        path.resolve("childA").resolve("grandChildB").resolve("greatGrandChild").resolve("foo").createIfNotExists()

        path.resolve("childB").resolve("grandChildA").resolve("greatGrandChild").resolve("foo").createIfNotExists()
        path.resolve("childB").resolve("grandChildB").resolve("greatGrandChild").resolve("bar").createIfNotExists()
        path.resolve("childC").createIfNotExists()
        path.resolve("childD").resolve("meh").resolve("xyz").createIfNotExists()

        val expected =
          """root
                         |     +- childA
                         |     😃      +- grandChildA
                         |     😃      😃        +- A
                         |     😃      😃        +- B
                         |     😃      😃           +- B1
                         |     😃      😃           +- B2
                         |     😃      😃        +- C
                         |     😃      😃        +- X
                         |     😃      😃           +- foo1
                         |     😃      😃           +- foo2
                         |     😃      😃           +- meh
                         |     😃      +- grandChildB
                         |     😃              +- A
                         |     😃              +- B
                         |     😃              +- greatGrandChild
                         |     😃                        +- foo
                         |     +- childB
                         |     😃      +- grandChildA
                         |     😃      😃        +- greatGrandChild
                         |     😃      😃                  +- foo
                         |     😃      +- grandChildB
                         |     😃              +- greatGrandChild
                         |     😃                        +- bar
                         |     +- childC
                         |     +- childD
                         |           +- meh
                         |               +- xyz""".stripMargin

        withClue(path.renderTree()) {
          check(path.renderTree(), expected)
        }
      }
    }
  }
}
