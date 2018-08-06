package eie
import scala.util.control.NonFatal

case class TryIterator[T](iter: Iterator[T])(onErr: PartialFunction[Throwable, Nothing]) extends Iterator[T] {
  override def hasNext: Boolean =
    try {
      iter.hasNext
    } catch {
      case NonFatal(e) if onErr.isDefinedAt(e) => onErr(e)
    }

  override def next(): T =
    try {
      iter.next
    } catch {
      case NonFatal(e) if onErr.isDefinedAt(e) => onErr(e)
    }
}
