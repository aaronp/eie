package eie.io

import com.typesafe.scalalogging.LazyLogging

import scala.util.control.NonFatal

/**
  * A wrapper for lazy values which can be queried whether they are created.
  *
  * This was created primarily for being able to close/clean up things which
  * may or may not have been created.
  *
  * e.g. instead of this:
  * {{{
  *   lazy val someResource = ...
  *
  *   // we may not have had a 'someResource' yet, so trying to close() our object
  *   // would instantiate and then 'close' the resource
  *   def close() = someResource.close()
  * }}}
  *
  * we'd have this:
  * {{{
  *
  *   private val someResource_ = Lazy { ... }
  *   def someResource = someResource_.value
  *
  *   def close() = someResource_.close() // or perhaps someResource_.foreach(_.destroy)
  * }}}
  *
  * @param mkValue
  * @tparam T
  */
final class Lazy[T](mkValue: => T) extends AutoCloseable with LazyLogging {

  @volatile private var _created = false
  lazy val value = {
    _created = true
    mkValue
  }

  /** use this resource
    * @param f the thunk to use the resource
    */
  def foreach(f: T => Unit) = {
    if (_created) {
      f(value)
    }
  }

  def created() = _created

  override def close(): Unit = foreach {
    case auto: AutoCloseable =>
      try {
        auto.close()
      } catch {
        case NonFatal(e) => logger.error(s"${auto} threw on close: ${e.getMessage}", e)
      }
    case _ =>
  }
}

object Lazy {
  def apply[T](value: => T): Lazy[T] = new Lazy(value)
}
