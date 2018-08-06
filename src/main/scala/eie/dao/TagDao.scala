package eie.dao

import java.nio.file.Path

import eie.dao.instances.FileTagDao
import eie.io.FromBytes

/**
  * Represents the ability to tag data with key/values
  *
  * @tparam T
  */
trait TagDao[T] {

  type SetResult

  def setTag(data: T, tag: String, value: String): SetResult

  def removeTags(data: T, tags: Set[String]): Unit

  def removeTag(data: T, tag: String): Unit = removeTags(data, Set(tag))

  def remove(data: T): Unit

  def valueForTag(data: T, tag: String): Option[String]

  def tagsFor(data: T): Set[String]

  def hasTag(data: T, tag: String): Boolean = tagsFor(data).contains(tag)

  /**
    * @param tag
    * @param value
    * @return all values which have the given tag and value
    */
  def findDataWithTagValue(tag: String, value: String): Iterator[T]

  /**
    * @param tag the tag to check
    * @return all values which have the given tag
    */
  def findDataWithTag(tag: String): Iterator[T]

}

object TagDao {
  def apply[T: Persist: HasId: FromBytes](dir: Path, maxTagLen: Int = 15, maxTagValueLen: Int = 20): FileTagDao[T] = {
    new FileTagDao[T](dir, maxTagLen, maxTagValueLen)
  }
}
