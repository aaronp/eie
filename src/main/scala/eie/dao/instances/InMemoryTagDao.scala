package eie.dao.instances
import eie.dao.TagDao

case class InMemoryTagDao[T]() extends TagDao[T] {
  override type SetResult = Boolean

  private object TagLock

  override def setTag(data: T, tag: String, value: String): Boolean = {
    ???
  }

  override def removeTags(data: T, tags: Set[String]): Unit = ???

  override def remove(data: T): Unit = ???

  override def valueForTag(data: T, tag: String): Option[String] = ???

  override def tagsFor(data: T): Set[String] = ???

  override def findDataWithTagValue(tag: String, value: String): Iterator[T] = ???

  override def findDataWithTag(tag: String): Iterator[T] = ???
}
