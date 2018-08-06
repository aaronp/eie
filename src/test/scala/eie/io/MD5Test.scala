package eie.io

class MD5Test extends BaseIOSpec {

  "MD5" should {
    val inputs = List("hello", "", "foo +{bar}", "meh")
    inputs.foreach { input =>
      s"hash '$input'" in {
        MD5(input) shouldBe MD5(input)
        MD5(input) should not be MD5(input + "x")
        MD5(input) should not be (input)
      }
    }
    "create smaller text" in {
      MD5("x" * 1000).length shouldBe 32
      MD5("x").length shouldBe 32
      MD5("").length shouldBe 32
    }
  }
}
