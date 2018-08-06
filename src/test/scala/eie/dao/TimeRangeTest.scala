package eie.dao

import java.time.LocalDate

import eie.BaseIOSpec


class TimeRangeTest extends BaseIOSpec {
  val date = LocalDate.of(1977, 7, 8)

  "TimeRange.completelyContainsDate" should {
    "return true for dates which are entirely in the range" in {
      val range = TimeRange(date.atTime(1, 2), date.atTime(1, 2).plusDays(2))
      range.completelyContainsDate(date) shouldBe false
      range.completelyContainsDate(date.plusDays(1)) shouldBe true
      range.completelyContainsDate(date.plusDays(2)) shouldBe false
    }
  }
  "TimeRange.completelyContainsDateAndHour" should {
    "return true for dates which are entirely in the range" in {
      TimeRange(date.atTime(1, 2), date.atTime(1, 3))
        .completelyContainsDateAndHour(date, 1) shouldBe false
      TimeRange(date.atTime(1, 2), date.atTime(2, 3))
        .completelyContainsDateAndHour(date, 1) shouldBe false
      TimeRange(date.atTime(1, 0), date.atTime(1, 59))
        .completelyContainsDateAndHour(date, 1) shouldBe false
      TimeRange(date.atTime(1, 0), date.atTime(2, 0))
        .completelyContainsDateAndHour(date, 1) shouldBe true
      TimeRange(date.atTime(1, 0), date.atTime(3, 0))
        .completelyContainsDateAndHour(date, 2) shouldBe true
    }
  }
}
