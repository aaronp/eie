package eie
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
  * Provides a means to encode a long into a shorter string of characters (alphanumeric by default,
  * so effectively base 62 instead of base 10 (10 + 26 + 26)
  */
object AlphaCounter {

  /**
    * provides an iterator which will produce increasing unique character strings
    *
    * @param seed  the seed from which to start
    * @param chars the character set from which to pull unique id characters
    * @return an iterator which produces sequential unique ids
    */
  def from(seed: Long = 0, chars: IndexedSeq[Char] = DefaultChars): BufferedIterator[String] = {
    new BufferedIterator[String] {
      private val counter = new AtomicLong(seed)

      override def head = apply(counter.get, chars)

      override def toString = head

      override def next = apply(counter.incrementAndGet, chars)

      override def hasNext = counter.get < Int.MaxValue
    }
  }

  private val maxCharCache = collection.mutable.HashMap[Int, Int]()

  def apply(baseTenNumber: Long, chars: IndexedSeq[Char] = DefaultChars): String = {

    val isPositive = baseTenNumber >= 0
    val (input, isMinValue) = if (isPositive) {
      baseTenNumber -> false
    } else {
      val mightBePositive = Int.MaxValue + baseTenNumber
      // edge case -  Int.MinValue.abs is Int.MinValue!
      if (mightBePositive < 0) {
        (baseTenNumber + 1).abs -> true
      } else {
        mightBePositive -> false
      }
    }

    val initial        = initialPower(input, chars.size)
    val str: Seq[Char] = next(chars, input, initial, chars.size, Nil)
    if (isPositive) {
      str.reverse.mkString("")
    } else {
      val maxCharLen = maxCharCache.getOrElseUpdate(chars.size, apply(Int.MaxValue, chars).length)
      val padChar    = if (isMinValue) chars.tail.head else chars.head
      str.padTo(maxCharLen + 1, padChar).reverse.mkString("")
    }
  }

  def apply(uuid: UUID): String = apply(uuid.hashCode)

  /**
    * @return the first exponent of 'chars' to use to produce a number larger than the user's input
    */
  private def initialPower(baseTenNumber: Long, base: Int): Int = {
    val window = Iterator.from(0).sliding(2, 1).map(_.toList)
    val powers = window.map {
      case (List(a, pow)) => (a, pow, Math.pow(base, pow))
    }
    powers
      .collectFirst {
        case (a, pow, exp) if exp == baseTenNumber => pow
        case (a, pow, exp) if exp > baseTenNumber  => a
      }
      .headOption
      .getOrElse(0)
  }

  @annotation.tailrec
  private def next(chars: IndexedSeq[Char], input: Long, pow: Int, base: Int, builder: Seq[Char]): Seq[Char] = {
    val (remaining: Long, appended: Seq[Char]) = if (input == 0) {
      0L -> (chars(0) +: builder)
    } else {
      val divisor   = Math.pow(base, pow).toLong
      val charIndex = (input / divisor).toInt
      (input % divisor) -> (chars(charIndex) +: builder)
    }

    if (pow == 0) {
      appended
    } else {
      next(chars, remaining, pow - 1, base, appended)
    }
  }

  val DefaultChars = (0 to 9).map(_.toString.head) ++ ('A' to 'z').filter(_.isLetterOrDigit)

}
