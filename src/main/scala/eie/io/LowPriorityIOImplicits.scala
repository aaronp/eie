package eie.io

import java.nio.file._
import scala.language.implicitConversions
import scala.language.postfixOps

trait LowPriorityIOImplicits {
  given Conversion[String, RichPath] = new RichPath(_ : String)

  given Conversion[Path, RichPath] = new RichPath(_ : Path)

  /**
    * exposes 'toPath' on strings, for e.g.
    * {{{
    *   "/var/tmp/foo.txt".asPath.text = "hello world"
    * }}}
    *
    * @return a pimped string
    */
  extension(path: String) {
    def asPath = Paths.get(path)
  }
}

/**
  * Contains some 'pimped' types (adding <string>.asPath), and RichPath
  */
object LowPriorityIOImplicits extends LowPriorityIOImplicits {

  val DefaultWriteOps: Set[OpenOption] = {
    Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC)
  }
}
