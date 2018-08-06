package eie

import java.nio.file.Path
import java.util.UUID

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.{implicitConversions, postfixOps}
import scala.util.Properties

/**
  * A base class for agora tests, exposing 'withDir' and some timeouts
  *
  * See http://www.scalatest.org/user_guide/defining_base_classes
  */
abstract class BaseIOSpec extends WordSpec with Matchers with ScalaFutures with LowPriorityIOImplicits with BeforeAndAfterAll {

  /**
    * All the timeouts!
    */
  implicit def testTimeout: FiniteDuration = 2.seconds

  /**
    * @return the timeout for something NOT to happen
    */
  def testNegativeTimeout: FiniteDuration = 300.millis

  def testClassName = getClass.getSimpleName.filter(_.isLetterOrDigit)

  implicit override def patienceConfig =
    PatienceConfig(timeout = scaled(Span(testTimeout.toSeconds, Seconds)), interval = scaled(Span(150, Millis)))

  implicit def richFuture[T](fut: Future[T]) = new {
    def block = Await.result(fut, testTimeout)
  }

  def srcDir: Path = BaseIOSpec.srcDir

  def withDir[T](f: Path => T): T = BaseIOSpec.withDir(getClass.getSimpleName)(f)

}

object BaseIOSpec extends LowPriorityIOImplicits {

  def nextTestDir(name: String) = {
    s"target/test/${name}-${UUID.randomUUID()}".asPath
  }

  def withDir[T](name: String)(f: Path => T): T = {
    val dir: Path = nextTestDir(name)
    if (dir.exists()) {
      dir.delete()
    }
    dir.mkDirs()
    try {
      f(dir)
    } finally {
      dir.delete()
    }
  }

  lazy val srcDir: Path = {
    def root(p: Path): Path = {
      if (p.fileName == "agora") {
        p
      } else {
        p.parent.map(root).getOrElse(sys.error("Hit file root looking for source root"))
      }
    }

    root(Properties.userDir.asPath).toAbsolutePath
  }
}
