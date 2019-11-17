package eie.io

import java.util.NoSuchElementException

import org.scalatest.{Matchers, WordSpec}

import scala.util.Try

class CloseableIteratorTest extends WordSpec with Matchers {

  "CloseableIterator.take" should {
    "still close the iterator" in {
      var closed = false
      val iter = CloseableIterator(Iterator(1, 2, 3)) {
        closed = true
      }
      iter.take(1).toList shouldBe List(1)
      closed shouldBe true
    }
  }
  "CloseableIterator.slice" should {
    "still close the iterator" in {
      var closed = false
      val iter = CloseableIterator(Iterator(1, 2, 3, 4, 5)) {
        closed = true
      }
      iter.slice(2, 3).toList shouldBe List(3)
      closed shouldBe true
    }
  }
  "CloseableIterator" should {
    "propagate exceptions after closing" in {
      object Bang extends Exception
      var closeCalled = false
      val iter = CloseableIterator(new Iterator[Int] {
        override def hasNext = throw Bang
        override def next()  = ???
      }) {
        closeCalled = true
      }
      closeCalled shouldBe false
      val bang = intercept[Bang.type] {
        iter.hasNext
      }
      bang shouldBe Bang
    }
    "invoke the close thunk when the iterator is exhausted" in {

      object Iter extends TwoInts
      val iter = CloseableIterator(Iter) {
        Iter.closed = true
      }
      Iter.closed shouldBe false
      iter.hasNext shouldBe true
      iter.next shouldBe 1
      iter.hasNext shouldBe true
      iter.next shouldBe 2
      Iter.closed shouldBe false
      // this call, checking for more, should then invoke the close thunk
      iter.hasNext shouldBe false
      Iter.closed shouldBe true
      iter.hasNext shouldBe false
    }
    "invoke the close thunk if hasNext throws an exception" in {

      var closedCalls = 0
      object Iter extends Iterator[Int] {
        override def hasNext: Boolean = sys.error("bang")

        override def next(): Int = 1
      }
      val iter = CloseableIterator(Iter) {
        closedCalls = closedCalls + 1
      }
      closedCalls shouldBe 0
      Try(iter.hasNext)
      closedCalls shouldBe 1

      // just close once
      Try(iter.hasNext)
      closedCalls shouldBe 1
    }
    "close the thunk and stop iteration if 'close' is called on the iterator" in {

      var closedCalls = 0
      val iter = CloseableIterator(Iterator.from(0)) {
        closedCalls = closedCalls + 1
      }
      closedCalls shouldBe 0
      iter.hasNext shouldBe true
      iter.next
      closedCalls shouldBe 0

      // now close the infinite iterator
      iter.close
      closedCalls shouldBe 1

      // try again ... the thunk should not be called again
      iter.close
      closedCalls shouldBe 1

      iter.hasNext shouldBe false
      intercept[NoSuchElementException] {
        iter.next()
      }
    }
  }

  class TwoInts extends Iterator[Int] {
    var closed = false
    var calls  = 0

    override def hasNext: Boolean = {
      calls < 2
    }

    override def next(): Int = {
      calls = calls + 1
      closed shouldBe (calls > 2)
      calls
    }
  }

}
