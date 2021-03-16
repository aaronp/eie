package eie.io

import java.io.{InputStream, OutputStream, OutputStreamWriter, PrintWriter}
import java.nio.charset.{Charset, StandardCharsets}
import java.nio.file._
import java.nio.file.attribute.{BasicFileAttributes, FileAttribute, FileTime, PosixFilePermission}
import java.time.{Instant, ZoneId, ZonedDateTime}
import java.util.function.BiPredicate
import java.util.stream

import scala.collection.JavaConverters._

/**
  * A DSL to make working with NIO paths a bit easier/fluid
  *
  * @param path
  */
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

  /** @param options
    * @return an input stream for this path
    */
  def inputStream(options: OpenOption*): InputStream = Files.newInputStream(path, options: _*)

  /** @return a lazy iterator of the path contents
    */
  def lines: Iterator[String] = Files.lines(path).iterator().asScala

  /** @return the parent path if this isn't the root path
    */
  def parent = Option(path.getParent)

  /** @return a stream of the parents
    */
  def parents: Stream[Path] = {
    parent match {
      case None    => Stream.empty
      case Some(p) => p #:: p.parents
    }
  }

  /** find the first matching path under this directory
    * @param depth the maximum depth to search to
    * @param p the file matching predicate
    * @return the first found path
    */
  def findFirst(depth: Int)(p: Path => Boolean): Option[Path] = {
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

  /** create this file if it does not already exist
    * @param atts the file attributes to use if the file must be created
    * @return this path
    */
  def createIfNotExists(atts: FileAttribute[_]*): Path = {
    if (!exists()) {
      mkParentDirs()
      Files.createFile(path, atts: _*)
    }
    path
  }

  /** Create the parent directories if required by this path
    * e.g. if the path is 'tmp/foo/bar.txt' then 'tmp/foo' will be created, but not bar.txt
    *
    * @param atts the directory attributes
    * @return this path
    */
  def mkParentDirs(atts: FileAttribute[_]*) = parent.foreach(_.mkDirs(atts: _*))

  /** Creates all required directories represented by this path
    *  @param atts the directory attributes
    * @return this path
    */
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

  /** @return the children if this path represents a directory
    */
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

  /** @return the path's attributes
    */
  def attributes: BasicFileAttributes = Files.readAttributes(path, classOf[BasicFileAttributes])

  /** @return the last modified time
    */
  def lastModified: FileTime = attributes.lastModifiedTime()

  /** @return the last modified time as epoch millis
    */
  def lastModifiedMillis: Long = lastModified.toMillis

  /** @return the path created instant
    */
  def created: Instant = attributes.creationTime.toInstant

  /** @return the created instant as a UTC date
    */
  def createdUTC: ZonedDateTime = created.atZone(ZoneId.of("UTC"))

  /** @return the file name
    */
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

  /** recursively (if set) delete this path
    * @param recursive set to true to recurse
    * @return this path
    */
  def delete(recursive: Boolean = true): Path = {
    if (isDir && recursive) {
      children.foreach(_.delete())
      Files.delete(path)
    } else {
      deleteFile()
    }
    path
  }

  /** @param followLinks
    * @return
    */
  def deleteFile(followLinks: Boolean = false): Path = {
    val exists = if (followLinks) path.exists() else path.exists(LinkOption.NOFOLLOW_LINKS)
    if (exists) {
      Files.delete(path)
    }
    path
  }

  /** @param dest the destination directory or file.
    * @param options the copy options
    * @return the moved path
    */
  def moveTo(dest: Path, options: CopyOption*) = {
    val target = if (dest.isDir) {
      dest.resolve(path.fileName)
    } else {
      dest
    }
    java.nio.file.Files.move(path, target, options: _*)
  }

  def copyTo(dest: Path, options: CopyOption*) = {
    val target = if (dest.isDir) {
      dest.resolve(path.fileName)
    } else {
      dest
    }
    java.nio.file.Files.copy(path, target, options: _*)
  }
}
