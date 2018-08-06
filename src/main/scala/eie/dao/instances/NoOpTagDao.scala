package eie.dao.instances
import eie.dao.TagDao

class NoOpTagDao[T] extends TagDao[T] {
  override type SetResult = Unit

  override def setTag(data: T, tag: String, value: String): Unit = {}

  override def removeTags(data: T, tags: Set[String]): Unit = {}

  override def remove(data: T): Unit = {}

  override def valueForTag(data: T, tag: String): Option[String] = None

  override def tagsFor(data: T): Set[String] = Set.empty

  override def findDataWithTagValue(tag: String, value: String): Iterator[T] = Iterator.empty

  override def findDataWithTag(tag: String): Iterator[T] = Iterator.empty
}
