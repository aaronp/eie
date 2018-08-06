package eie
import simulacrum.typeclass

/**
  * Typeclass to serialize a type to bytes
  *
  * @tparam T the value to convert
  */
@typeclass trait ToBytes[T] {

  /**
    * Converts the T to bytes
    *
    * @param value the value to convert
    * @return the byte array representing this value
    */
  def bytes(value: T): Array[Byte]

  def contramap[A](f: A => T): ToBytes[A] = {
    val parent = this
    new ToBytes[A] {
      override def bytes(value: A): Array[Byte] = {
        parent.bytes(f(value))
      }
    }
  }
}

object ToBytes extends LowPriorityIOImplicits {

  implicit object Utf8String extends ToBytes[String] {
    override def bytes(value: String): Array[Byte] = value.getBytes("UTF-8")
  }

  implicit object IntToBytes extends ToBytes[Int] {
    def toInt(bytes: Array[Byte]): Int = {
      val (result, _) = bytes.foldRight((0, 0)) {
        case (nextByte, (builder, shift)) =>
          val x: Int = nextByte << shift
          (builder + x, shift + 1)
      }
      result
    }
    override def bytes(value: Int): Array[Byte] = {
      val (list, x) = (0 until 8).foldLeft((List[Byte]() -> value)) {
        case ((array, remaining), _) =>
          val value        = remaining.toByte
          val newRemaining = remaining >> 4
          (value :: array, newRemaining)
      }
      list.toArray
    }
  }

}
