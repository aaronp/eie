package eie.dao

import java.nio.file.Path

import eie.dao.instances.FileIdDao
import eie.io.FromBytes

/** An interface representing a key/value store
  *
  * @tparam ID the id type
  * @tparam T  the value type
  */
trait IdDao[ID, T] {

  type Result
  type RemoveResult

  /**
    * Saved data against an ID
    *
    * @param id    the ID against which the value should be saved
    * @param value the value to save
    * @return the save result
    */
  def save(id: ID, value: T): Result

  /**
    * @param id the id of the value to remove
    * @return the remove result
    */
  def remove(id: ID): RemoveResult

  /** @param id the id of the value to retrieve
    * @return the value in an option
    */
  def get(id: ID): Option[T]

  def contains(id: ID): Boolean = get(id).isDefined
}

object IdDao {
  def apply[T: Persist: FromBytes](dir: Path): FileIdDao[T] = new FileIdDao[T](dir)
}
