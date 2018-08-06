package eie.dao

import java.nio.file.Path

import eie.io.ToBytes
import eie.io._

/**
  * A means of writing some data to a path
  *
  * @tparam T the type of the thing to write
  */
trait Persist[T] {

  /**
    * Writes the value to the given file
    *
    * @param file  the file to save to
    * @param value the value to write
    */
  def write(file: Path, value: T): Unit
}

object Persist {

  def apply[T](f: (Path, T) => Unit): Persist[T] = new Persist[T] {
    override def write(file: Path, value: T): Unit = f(file, value)
  }

  implicit def writer[T: ToBytes] = new WriterInstance[T](implicitly[ToBytes[T]])

  def save[T: ToBytes](): Persist[T] = apply {
    case (file, value) => file.bytes = implicitly[ToBytes[T]].bytes(value)
  }

  def link[T](linkToThisFile: Path): Linker[T] = Linker(linkToThisFile)

  class WriterInstance[T](toBytes: ToBytes[T]) extends Persist[T] {
    override def write(file: Path, value: T): Unit = {
      file.bytes = toBytes.bytes(value)
    }
  }

  case class Linker[T](linkToThisFile: Path) extends Persist[T] {
    override def write(file: Path, value: T): Unit = {
      linkToThisFile.createSymbolicLinkFrom(file)
    }
  }

}
