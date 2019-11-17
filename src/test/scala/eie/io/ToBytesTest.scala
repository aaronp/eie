package eie.io
import eie.io.ToBytes.IntToBytes

class ToBytesTest extends BaseIOSpec {

  "IntToBytes.contramap" should {
    "map inputs" in {
      val intToBytes = ToBytes.Utf8String.contramap[Int](_.toString)
      intToBytes.bytes(1234) shouldBe "1234".toString.getBytes()
    }
  }
  "IntToBytes" should {
    List(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 63, 64, 65, 100, -100).foreach { expected =>
      s"convert $expected (${expected.toBinaryString}) to bytes" in {
        val bytes = IntToBytes.bytes(expected)
        IntToBytes.toInt(bytes) shouldBe expected
      }
    }
  }
}
