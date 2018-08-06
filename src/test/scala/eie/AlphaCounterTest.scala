package eie

class AlphaCounterTest extends BaseIOSpec {

  "AlphaCounter" should {
    "produce unique ids" in {
      val c = AlphaCounter.from(Int.MaxValue * 10)
      c.next() shouldBe "02LKcas"
      c.next() shouldBe "02LKcat"
      c.next() shouldBe "02LKcau"
      AlphaCounter(1519718890137L) shouldBe "Qkq9aUD"
      AlphaCounter(3519718890137L) shouldBe "zxvhleL"
    }
  }
}
