package eie.io

import scala.util.Success

class FromBytesTest extends BaseIOSpec {

  "FromBytes.map" should {
    "map bytes" in {
      implicit val fromBytes: FromBytes[String] = FromBytes.Utf8String.map(_.reverse)

      val text = "original string"
      FromBytes[String].read(text.getBytes) shouldBe Success(text.reverse)
    }
  }
  "FromBytes.lift" should {
    "create an instance from a function" in {
      FromBytes.lift(_ => "constant").read(Array[Byte](1, 2, 3)) shouldBe Success("constant")
    }
  }
}
