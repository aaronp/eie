package eie
import java.io.{File, FilenameFilter}

case class FileNamePredicate(val filter: String => Boolean) extends FilenameFilter {
  override def accept(dir: File, name: String): Boolean = filter(name)
}
