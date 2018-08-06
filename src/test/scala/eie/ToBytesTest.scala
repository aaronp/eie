package eie
import agora.io.ToBytes.IntToBytes

class ToBytesTest extends BaseIOSpec {

  "IntToBytes" should {
    List(Integer.MIN_VALUE, Integer.MAX_VALUE, -1, 0, 1, 63, 64, 65, 100, -100).foreach { expected =>
      s"convert $expected to bytes" in {
        val bytes = IntToBytes.bytes(expected)
        IntToBytes.toInt(bytes) shouldBe expected
      }
    }
  }
}
