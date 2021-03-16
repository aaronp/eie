package eie.io

import java.nio.file._
import scala.language.implicitConversions

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
  extension(path: String)
    def asPath = Paths.get(path)

  given Conversion[String, RichPath] = new RichPath(_)

  given Conversion[Path, RichPath] = new RichPath(_)
}

object LowPriorityIOImplicits:
  val DefaultWriteOps: Set[OpenOption] = {
    Set(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DSYNC)
  }
end LowPriorityIOImplicits
