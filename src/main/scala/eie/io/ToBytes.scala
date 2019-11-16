package eie.io

import java.nio.ByteBuffer

/**
  * Typeclass to serialize a type to bytes
  *
  * @tparam T the value to convert
  */
trait ToBytes[T] {

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

  def apply[T](implicit inst: ToBytes[T]): ToBytes[T] = inst

  implicit object Utf8String extends ToBytes[String] {
    override def bytes(value: String): Array[Byte] = value.getBytes("UTF-8")
  }

  implicit object IntToBytes extends ToBytes[Int] {
    def toInt(bytes: Array[Byte]): Int = {
      val buff = ByteBuffer.wrap(bytes)
      buff.getInt(0)
    }
    override def bytes(value: Int): Array[Byte] = {
      val buff = ByteBuffer.allocate(Integer.BYTES)
      buff.putInt(value)
      buff.flip()
      buff.array()
    }
  }

}
