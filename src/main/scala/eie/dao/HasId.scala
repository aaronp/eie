package eie.dao
import simulacrum.typeclass

/**
  * @tparam T the type for which there's an ID
  */
@typeclass trait HasId[T] {
  def id(value: T): String
}
object HasId {
  def lift[T](f: T => String): HasId[T] = new HasId[T] { override def id(value: T): String = f(value) }
}
