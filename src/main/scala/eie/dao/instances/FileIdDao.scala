package eie.dao.instances

import java.nio.file.Path

import eie.dao.{IdDao, Persist}
import eie.io.FromBytes
import eie.io._

class FileIdDao[T: Persist: FromBytes](dir: Path) extends IdDao[String, T] {
  override type Result       = Path
  override type RemoveResult = Path

  private val fromBytes = FromBytes[T]
  private val persist   = implicitly[Persist[T]]

  override def save(id: String, value: T) = {
    val file = dir.resolve(id)
    persist.write(file, value)
    file
  }

  /** @param id the id of the value to remove
    * @return the remove result
    */
  override def remove(id: String) = dir.resolve(id).delete()

  /** @param id the data to check
    * @return true if we've heard about 'id'
    */
  override def contains(id: String): Boolean = getFile(id).isDefined

  override def get(id: String) = {
    getFile(id).flatMap { file =>
      fromBytes.read(file.bytes).toOption
    }
  }

  /** @param id the id to retrieve
    * @return the file which contains the data for the given ID
    */
  def getFile(id: String): Option[Path] = {
    val file = dir.resolve(id)
    if (file.exists()) {
      Option(file)
    } else {
      None
    }
  }
}
