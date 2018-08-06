package eie.dao.instances
import java.nio.file.Path

import eie.FromBytes
import eie.implicits._
import eie.dao.{HasId, Persist, TagDao}

import scala.util.Try

/**
  * Stores things in the format:
  *
  *
  * When a tag value is not alphanumeric, or is longer than 'maxTagValueLen':
  *
  * <root dir>/tags/<tag>/<value hash>/<id>/.value = <value>
  * <root dir>/tags/<tag>/<value hash>/<id>/.data = <data>
  *
  * or, when the tag value IS alphanumber and within 'maxTagValueLen':
  *
  * <root dir>/tags/<tag>/<value>/<id>/.data
  *
  * It also stores:
  *
  * <root dir>/ids/<id>/<tag> = <value>
  *
  * for queries on just the id (e.g. list tags), or when the id and tag are known
  *
  * @param rootDir   the directory to store the data
  * @param maxTagLen the maximum allowed length of tags
  * @param persist   the ability to persist T values
  * @param hasId     the ability to determine an ID for T values
  * @param fromBytes the ability to read T values from byte arrays
  * @tparam T the value type
  */
case class FileTagDao[T](rootDir: Path, maxTagLen: Int, maxTagValueLen: Int)(
    implicit
    persist: Persist[T],
    hasId: HasId[T],
    fromBytes: FromBytes[T]
) extends TagDao[T] {

  override type SetResult = Path

  private val tagsDir = rootDir.resolve("tags")
  private val idsDir  = rootDir.resolve("ids")

  override def findDataWithTag(tag: String) = {
    idFilesWithTag(tag).flatMap { file =>
      read(file.resolve(".data"))
    }
  }

  private def idFilesWithTag(tag: String) = tagsDir.resolve(tag).nestedFiles(1)

  def findIdsWithTag(tag: String) = idFilesWithTag(tag).map(_.fileName)

  private def isValidTag(tag: String) = {
    tag.length <= maxTagLen && tag.forall {
      case ' ' => true
      case c   => c.isLetterOrDigit
    }
  }

  override def setTag(data: T, tag: String, tagValue: String) = {
    require(isValidTag(tag), s"Tags must be alphanumeric and no longer than $maxTagLen characters : '$tag'")
    removeTag(data, tag)
    val id = hasId.id(data)
    TagIdDir(id, tag, tagValue).write(data)
    writeInIdsFile(id, data, tag, tagValue)
  }

  override def tagsFor(data: T) = tagsForId(hasId.id(data))

  def tagsForId(id: String) = {
    idsDir.resolve(id).children.map(_.fileName).toSet
  }

  override def hasTag(data: T, tag: String): Boolean = {
    val id = hasId.id(data)
    idsDir.resolve(id).resolve(tag).exists()
  }

  override def findDataWithTagValue(tag: String, tagValue: String): Iterator[T] = {
    findDataFilesWithTagValue(tag, tagValue).flatMap { dir =>
      read(dir.resolve(".data"))
    }
  }

  def findDataFilesWithTagValue(tag: String, tagValue: String): Iterator[Path] = {
    val base = tagsDir.resolve(tag)
    if (useValueVerbatim(tagValue)) {
      base.resolve(tagValue).childrenIter
    } else {
      val valueDir = base.resolve(tagValue.hashCode.toString)
      valueDir.childrenIter.filter { dir =>
        dir.resolve(".value").text == tagValue
      }
    }
  }

  private def read(file: Path): Option[T] = {
    fromBytes.read(file.bytes).toOption
  }

  override def removeTags(data: T, tags: Set[String]) = {
    val id = hasId.id(data)
    tags.foreach { tag =>
      valueForTag(data, tag).foreach { tagValue =>
        TagIdDir(id, tag, tagValue).removeId(id)
      }

      val tagFile = idsDir.resolve(id).resolve(tag)
      tagFile.delete()
    }
  }

  override def valueForTag(data: T, tag: String): Option[String] = {
    val id   = hasId.id(data)
    val file = idsDir.resolve(id).resolve(tag)
    if (file.exists()) {
      Try(file.text).toOption
    } else {
      None
    }
  }

  override def remove(data: T): Unit = {
    val id   = hasId.id(data)
    val tags = tagsForId(id)
    tags.foreach { tag =>
      valueForTag(data, tag).foreach { tagValue =>
        resolveTagDir(id, tag, tagValue).foreach(_.delete())
      }
    }
  }

  private def writeInIdsFile(id: String, data: T, tag: String, tagValue: String) = {
    idsDir.resolve(id).resolve(tag).text = tagValue
  }

  private def resolveTagDir(id: String, tag: String, tagValue: String): Option[Path] = {
    TagIdDir(id, tag, tagValue).valueDir
  }

  def useValueVerbatim(tagValue: String): Boolean = {
    tagValue.length < maxTagValueLen && tagValue.forall(_.isLetterOrDigit)
  }

  /** If the value is hashed, then we have
    *
    * '''
    * <root dir>/tags/<tag>/<value hash>/<id>/.data
    * <root dir>/tags/<tag>/<value hash>/<id>/.value
    * '''
    *
    * Otherwise if the value is alphanumeric, we just have
    *
    * '''
    * <root dir>/tags/<tag>/<value>/<id>/.data
    * '''
    *
    */
  private case class TagIdDir(id: String, tag: String, tagValue: String) {

    /** @return the directory at the root of the .data and .value directory
      */
    def valueDir: Option[Path] = {
      if (usesVerbatim) {
        Option(idDir).filter(_.exists())
      } else {
        idDir.children.find(_.resolve(".value").text == tagValue)
      }
    }

    def removeId(id: String) = {
      idDir.delete()
      if (tagValueDir.children.isEmpty) {
        tagValueDir.delete()
      }
    }

    /**
      * Write down
      *
      * {{{
      *   <root dir>/tags/<tag>/<value or hash>/<id>/.data = <data>
      * }}}
      *
      * and optionally
      *
      * {{{
      *   <root dir>/tags/<tag>/<value or hash>/<id>/.value = <value>
      * }}}
      */
    def write(data: T) = {
      val valueDir: Path = {
        if (usesVerbatim) {
          idDir
        } else {
          val dir = idDir
          dir.resolve(".value").text = tagValue
          dir
        }
      }

      val tagsFile = valueDir.resolve(".data")
      persist.write(tagsFile, data)
      tagsFile
    }

    def usesVerbatim: Boolean = useValueVerbatim(tagValue)

    private lazy val idDir: Path = tagValueDir.resolve(id)

    private def tagValueDir: Path = {
      val valueOrHashDir = if (usesVerbatim) {
        tagsDir.resolve(tag).resolve(tagValue)
      } else {
        tagsDir.resolve(tag).resolve(tagValue.hashCode.toString)
      }
      valueOrHashDir
    }
  }

}
