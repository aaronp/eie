package eie.io

import java.io.{OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file._
import java.nio.file.attribute.{BasicFileAttributes, FileAttribute, FileTime, PosixFilePermission}
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.function.BiPredicate
import java.util.stream

import scala.compat.Platform

object RichPath {
  implicit def asRichPath(p: Path) = new RichPath(p)

}
class RichPath(val path: Path) {

  import RichPath._

  def defaultWriteOpts: Set[OpenOption] = LowPriorityIOImplicits.DefaultWriteOps

  /** @return the path rendered as a tree
    */
  def renderTree(): String = PathTreeNode(path, None).asTree().mkString(Platform.EOL)

  def renderTree(filter: Path => Boolean): String = PathTreeNode(path, Option(filter)).asTree().mkString(Platform.EOL)

  import scala.collection.JavaConverters._

  /** @return how many parents this path has
    */
  def depth: Int = parent.fold(1)(_.depth + 1)

  def setText(str: String, charset: Charset = StandardCharsets.UTF_8, options: Set[OpenOption] = defaultWriteOpts) = {
    setBytes(str.getBytes(charset), options)
  }

  def setBytes(bytes: Array[Byte], options: Set[OpenOption] = defaultWriteOpts) = {
    Files.write(path, bytes, options.toArray: _*)
    path
  }

  def createSymbolicLinkTo(file: Path, atts: FileAttribute[_]*) = {
    Files.createSymbolicLink(path.toAbsolutePath, file.toAbsolutePath, atts: _*)
    path
  }

  /**
    * Creates a symbolic link to the link from the path
    *
    * @param link the symbolic link to create
    * @return the link
    */
  def createSymbolicLinkFrom(link: Path, atts: FileAttribute[_]*) = {
    if (link.parent.exists(!_.exists())) {
      link.mkParentDirs()
    }
    Files.createSymbolicLink(link.toAbsolutePath, path.toAbsolutePath, atts: _*)
  }

  def createHardLinkFrom(link: Path) = {
    if (link.parent.exists(!_.exists())) {
      link.mkParentDirs()
    }
    Files.createLink(link, path)
  }

  def bytes = if (exists()) Files.readAllBytes(path) else Array.empty[Byte]

  def bytes_=(content: Array[Byte]) = {
    createIfNotExists()
    setBytes(content)
  }

  def text_=(str: String): Path = {
    createIfNotExists()
    setText(str)
  }

  def getText(charset: Charset = StandardCharsets.UTF_8): String = new String(bytes, charset)

  def text: String = getText()

  def append(text: String): Path =
    withOutputStream(_.write(text.getBytes))(Set(StandardOpenOption.APPEND))

  def withOutputStream(withOS: OutputStream => Unit)(implicit options: Set[OpenOption]): Path = {
    val os = outputStream(options.toList: _*)
    try {
      withOS(os)
    } finally {
      os.flush()
      os.close()
    }
    path
  }

  def search(maxDepth: Int, followLinks: Boolean = true)(p: Path => Boolean): Iterator[Path] = {
    val predicate = new BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, ignored: BasicFileAttributes): Boolean = p(t)
    }
    val found: stream.Stream[Path] = if (followLinks) {
      Files.find(path, maxDepth, predicate, FileVisitOption.FOLLOW_LINKS)
    } else {
      Files.find(path, maxDepth, predicate)
    }

    import scala.collection.JavaConverters._

    found.iterator().asScala
  }

  /**
    * a convenience method for searching under this path
    *
    * @param p the predicate to use
    * @return the found paths
    */
  def find(p: Path => Boolean) = search(Int.MaxValue, false)(p)

  def outputStream(options: OpenOption*): OutputStream = Files.newOutputStream(path, options: _*)

  def outputWriter(autoFlush: Boolean, options: OpenOption*) = {
    val writer = new OutputStreamWriter(outputStream(options: _*))
    new PrintWriter(writer, autoFlush)
  }

  def inputStream(options: OpenOption*) = Files.newInputStream(path, options: _*)

  def lines: Iterator[String] = Files.lines(path).iterator().asScala

  def parent = Option(path.getParent)

  def parents: Stream[Path] = {
    parent match {
      case None    => Stream.empty
      case Some(p) => p #:: p.parents
    }
  }

  def isEmptyDir = {
    isDir && children.isEmpty
  }

  def findFirst(depth: Int)(p: Path => Boolean) = {
    object check extends BiPredicate[Path, BasicFileAttributes] {
      override def test(t: Path, u: BasicFileAttributes): Boolean = {
        p(t)
      }
    }

    val stream = Files.find(path, depth, check)
    val jOpt   = stream.findFirst()
    if (jOpt.isPresent) {
      Option(jOpt.get)
    } else {
      None
    }
  }

  def createIfNotExists(atts: FileAttribute[_]*): Path = {
    if (!exists()) {
      mkParentDirs()
      Files.createFile(path, atts: _*)
    }
    path
  }

  def mkParentDirs(atts: FileAttribute[_]*) = parent.foreach(_.mkDirs(atts: _*))

  def mkDirs(atts: FileAttribute[_]*): Path = Files.createDirectories(path, atts: _*)

  def mkDirs(atts: Set[FileAttribute[_]]): Path = Files.createDirectories(path, atts.toList: _*)

  def mkDir(atts: FileAttribute[_]*): Path = {
    if (!exists()) Files.createDirectory(path, atts: _*) else path
  }

  def setFilePermissions(permission: PosixFilePermission, theRest: PosixFilePermission*): Path = {
    setFilePermissions(theRest.toSet + permission)
  }

  def setFilePermissions(permissions: Set[PosixFilePermission]): Path = {
    Files.setPosixFilePermissions(path, permissions.asJava)
  }

  def grantAllPermissions: Path = setFilePermissions(PosixFilePermission.values().toSet)

  def size = Files.size(path)

  def touch(attributes: FileAttribute[_]*) = Files.createFile(path, attributes: _*)

  def exists(linkOpts: LinkOption*) = Files.exists(path, linkOpts: _*)

  def isDir = exists() && Files.isDirectory(path)

  def isFile = exists() && Files.isRegularFile(path)

  /** @return all files under the given directory
    */
  def nestedFiles(depth: Int = Int.MaxValue): Iterator[Path] = {
    if (isFile) {
      Iterator(path)
    } else if (isDir) {
      if (depth == 0) {
        childrenIter
      } else {
        childrenIter.flatMap(_.nestedFiles(depth - 1))
      }
    } else {
      Iterator.empty
    }
  }

  def children: Array[Path] = if (isDir) path.toFile.listFiles().map(_.toPath) else Array.empty

  def childrenMatchingName(filenameFilter: String => Boolean): Array[Path] = {
    if (isDir) {
      val pred     = FileNamePredicate(filenameFilter)
      val filtered = path.toFile.listFiles(pred).map(_.toPath)
      filtered
    } else Array.empty
  }

  def childrenIter = if (isDir) Files.list(path).iterator().asScala else Iterator.empty

  def attributes: BasicFileAttributes = Files.readAttributes(path, classOf[BasicFileAttributes])

  def lastModified: FileTime = attributes.lastModifiedTime()

  def lastModifiedMillis = lastModified.toMillis

  def created = attributes.creationTime.toInstant

  def createdUTC = created.atZone(ZoneId.of("UTC"))

  def createdString = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(createdUTC)

  def fileName = path.getFileName.toString

  def delete(recursive: Boolean = true): Path = {
    if (isDir && recursive) {
      children.foreach(_.delete())
      Files.delete(path)
    } else {
      deleteFile()
    }
    path
  }

  def deleteFile(followLinks: Boolean = false): Path = {
    val exists = if (followLinks) path.exists() else path.exists(LinkOption.NOFOLLOW_LINKS)
    if (exists) {
      Files.delete(path)
    }
    path
  }

  def moveTo(dest: Path, options: CopyOption*) = {
    java.nio.file.Files.move(path, dest, options: _*)
  }
}
