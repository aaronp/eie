package eie.io

import java.io.{OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file._
import java.nio.file.attribute.{BasicFileAttributes, FileAttribute, FileTime, PosixFilePermission}
import java.time.format.DateTimeFormatter
import java.time.{ZoneId, ZonedDateTime}
import java.util.function.BiPredicate
import java.util.stream

import scala.collection.JavaConverters._

object RichPath {
  implicit def asRichPath(p: Path) = new RichPath(p)

}
final class RichPath(val path: Path) {

  def defaultWriteOpts: Set[OpenOption] = LowPriorityIOImplicits.DefaultWriteOps

  /** @return the path rendered as a tree
    */
  def renderTree(): String = PathTreeNode(path, None).asTree().mkString(System.lineSeparator)

  def renderTree(filter: Path => Boolean): String = PathTreeNode(path, Option(filter)).asTree().mkString(System.lineSeparator)

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

  /** @return the bytes of this file or empty if ths file doesn't exist
    */
  def bytes = if (exists()) Files.readAllBytes(path) else Array.empty[Byte]

  /**
    * Supports "someFile.txt".asPath.byte = Array[Byte](1,2,3)
    * @param content the content of the file to set
    * @return this path
    */
  def bytes_=(content: Array[Byte]) = {
    createIfNotExists()
    setBytes(content)
  }

  /**
    * Supports "someFile.txt".asPath.text = "some content"
    * The file will be created if it doesn't exist
    * @param content the content of the file to set
    * @return this path
    */
  def text_=(content: String): Path = {
    createIfNotExists()
    setText(content)
  }

  /** @param charset the charset of the file
    * @return the text of the file using the charset
    */
  def getText(charset: Charset = StandardCharsets.UTF_8): String = new String(bytes, charset)

  /** @return the text of the file using the charset
    */
  def text: String = getText()

  /**
    * @param text the text to append
    * @return this file (builder pattern) with the text appended
    */
  def append(text: String): Path = {
    withOutputStream { os =>
      os.write(text.getBytes)
      path
    }(Set(StandardOpenOption.APPEND))
  }

  /**
    * @param withOS the function which operates on this output stream
    * @param options the file options for opening the stream
    * @return the result of the
    */
  def withOutputStream[A](withOS: OutputStream => A)(implicit options: Set[OpenOption]): A = {
    val os = outputStream(options.toList: _*)
    try {
      withOS(os)
    } finally {
      os.flush()
      os.close()
    }
  }

  /** @param maxDepth the maximum file depth to file
    * @param followLinks should we follow symbolic/hard links?
    * @param p the file matching predicate
    * @return the files under this directory which match the predicate
    */
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

  def setFilePermissions(permission: PosixFilePermission, theRest: PosixFilePermission*): Path = {
    setFilePermissions(theRest.toSet + permission)
  }

  def setFilePermissions(permissions: Set[PosixFilePermission]): Path = {
    Files.setPosixFilePermissions(path, permissions.asJava)
  }

  def grantAllPermissions: Path = setFilePermissions(PosixFilePermission.values().toSet)

  /** @param linkOpts
    * @return the file permissions
    */
  def permissions(linkOpts: LinkOption*): Set[PosixFilePermission] = {
    Files.getPosixFilePermissions(path, linkOpts: _*).asScala.toSet
  }

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

  /** @param filenameFilter a filename predicate
    * @return the children of this directory which match the filename, or an empty path if this is not a directory
    */
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

  def createdUTC: ZonedDateTime = created.atZone(ZoneId.of("UTC"))

  def createdString = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(createdUTC)

  def fileName = path.getFileName.toString

  /**
    * This function supports the syntax:
    * {{{
    *   "path/to/some/file.txt".asPath
    * }}}
    * @param thunk some function which operates on a file
    * @tparam T
    * @return the result of the function
    */
  def deleteAfter[T](thunk: Path => T): T = {
    try {
      thunk(path)
    } finally {
      delete()
    }

  }
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
    val target = if (dest.isDir) {
      dest.resolve(path.fileName)
    } else {
      dest
    }
    java.nio.file.Files.move(path, target, options: _*)
  }
}
