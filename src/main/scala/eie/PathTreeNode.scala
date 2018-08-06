package eie

import java.nio.file.Path
import eie.implicits._

/**
  * A means to pretty-print a file system layout
  */
private[eie] trait PathTreeNode {

  /**
    * Creates a tree listing of a directory as:
    *{{{
    * <path>
    *    +- <child1>
    *    +- <child2>
    *    +- <child3>
    *    |     +- <grandchild1>
    *    |     +- <grandchild2>
    *    |     |        +- <greatGrandchild1>
    *    |     |        |         +- <dave>
    *    |     |        +- <greatGrandchild2>
    *    |     |                  +- <meh>
    *    |     +- <grandchild3>
    *    |              +- <foo>
    *    +- <child4>
    * }}}
    * @return a list of the output lines
    */
  def asTree(indent: String = "", isLastChild: Boolean = false): Iterable[String]

}

private[eie] object PathTreeNode {
  val OK = (_: Path) => true

  def apply(path: Path, filterOpt: Option[Path => Boolean]): PathTreeNode = {
    if (path.isDir) {
      PathTreeDir(path, filterOpt)
    } else {
      PathTreeLeaf(path)
    }
  }
}
private[eie] case class PathTreeLeaf(leaf: Path) extends PathTreeNode {
  override def asTree(indent: String = "", isLastChild: Boolean = false) = List(s"$indent+- ${leaf.fileName}")
}
private[eie] case class PathTreeDir(dir: Path, filterOpt: Option[Path => Boolean]) extends PathTreeNode {
  def children: Array[PathTreeNode] = {
    val all  = dir.children
    val kids = filterOpt.fold(all)(p => all.filter(p))

    kids.sortBy(_.fileName).map(c => PathTreeNode(c, filterOpt))
  }

  /**
    * @param indent the left hand side of the tree to render
    * @param isLastChild a flag so we know if we should include a pipe in any indents we provide
    * @return a list of the output lines
    */
  override def asTree(indent: String = "", isLastChild: Boolean = false) = {
    val name = dir.fileName

    // how big a space to put in underneath this filename
    val halfName = " " * ((name.length / 2) + 3)

    // the root directory doesn't get a '+- ' prefix, or provide a pipe '|' in its indent
    val (thisLine, newIndent) = indent match {
      case "" => name -> halfName
      case _ =>
        val bar = if (!isLastChild && dir.children.exists(_.isDir)) "|" else ""
        s"$indent+- $name" -> s"$indent${bar}$halfName"
    }

    val kids      = children
    val lastIndex = kids.size - 1

    thisLine +: children.zipWithIndex.flatMap {
      case (kid, i) => kid.asTree(newIndent, i == lastIndex)
    }
  }

}
