package eie.dao

/**
  * @tparam T the type for which there's an ID
  */
trait HasId[T] {
  def id(value: T): String
}

object HasId {
  def instance[T: HasId]: HasId[T] = implicitly[HasId[T]]

  trait LowPriorityHasIdImplicits {
    implicit def hasIdIdentity: HasId[String] = HasId.identity
  }
  object implicits extends LowPriorityHasIdImplicits

  case object identity extends HasId[String] {
    override def id(value: String): String = value
  }

  def lift[T](f: T => String) = new HasId[T] {
    override def id(value: T) = f(value)
  }
}
