package eie.io

import java.nio.file._

/**
  * Contains some 'pimped' types (adding <string>.asPath), and RichPath
  */
trait LowPriorityIOImplicits {

  /**
    * exposes 'toPath' on strings, for e.g.
    * {{{
    *   "/var/tmp/foo.txt".asPath.text = "hello world"
    * }}}
    *
    * @return a pimped string
    */
  implicit class RichPathString(val path: String) {
    def asPath = Paths.get(path)
  }

  implicit def asRichPath(path: Path) = new RichPath(path)

}

object LowPriorityIOImplicits {

  val DefaultWriteOps: Set[OpenOption] = {
    Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC)
  }
}
