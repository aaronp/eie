package eie.io

class HexTest extends BaseIOSpec {

  "Hex" should {

    "hex encode 'a' as 92" in {
      Hex(Array('a')) shouldBe "61"
    }
    "hex encode 'A' as 41" in {
      Hex(Array('A')) shouldBe "41"
    }
    "hex encode '0' as 30" in {
      Hex(Array('0')) shouldBe "30"
    }
    "hex encode '0' as 00" in {
      Hex(Array('0')) shouldBe "30"
    }
    "hex encode '10' as 0A" in {
      Hex(Array(10.toByte)) shouldBe "0A"
    }
    "hex encode '16' as 10" in {
      Hex(Array(16.toByte)) shouldBe "10"
    }
    "hex encode '127' as 7F" in {
      Hex(Array(127.toByte)) shouldBe "7F"
    }
  }
}
