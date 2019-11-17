package eie.io

import scala.util.{Success, Try}

/**
  * Typeclass to serialize a type from a byte array
  *
  * @tparam T
  */
trait FromBytes[T] {

  /**
    * Unmarshalls the byte array into the given type
    *
    * @param bytes the bytes to unmarshall
    * @return the T wrapped in a Try
    */
  def read(bytes: Array[Byte]): Try[T]

  def map[A](f: T => A): FromBytes[A] = {
    val parent = this
    new FromBytes[A] {
      override def read(bytes: Array[Byte]): Try[A] = parent.read(bytes).map(f)
    }
  }
}

object FromBytes {

  def apply[T](implicit inst: FromBytes[T]): FromBytes[T] = inst

  def lift[T](f: Array[Byte] => T) = new FromBytes[T] {
    override def read(bytes: Array[Byte]) = Try(f(bytes))
  }

  implicit object Utf8String extends FromBytes[String] {
    override def read(bytes: Array[Byte]): Try[String] = {
      Success(new String(bytes, "UTF-8"))
    }
  }

}
