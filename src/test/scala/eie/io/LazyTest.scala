package eie.io

import java.util.concurrent.atomic.AtomicInteger

class LazyTest extends BaseIOSpec {
  "Lazy.close" should {

    "not propagate exceptions thrown when closing" in {
      object Foo extends AutoCloseable {
        override def close(): Unit = {
          sys.error("bang")
        }
      }

      val foo = Lazy(Foo)
      foo.value shouldBe Foo
      foo.close()
      foo.close()
    }
    "close its value if it was created" in {
      object Foo extends AutoCloseable {
        var closed = false

        override def close(): Unit = {
          closed = true
        }
      }

      val foo = Lazy(Foo)
      Foo.closed shouldBe false
      foo.value shouldBe Foo
      foo.close()
      Foo.closed shouldBe true
    }
    "not close its value if it never was created" in {
      object Foo extends AutoCloseable {
        var closed = false

        override def close(): Unit = {
          closed = true
        }
      }

      val foo = Lazy(Foo)
      Foo.closed shouldBe false
      foo.close()
      Foo.closed shouldBe false
    }
  }
  "Lazy.value" should {
    "not create its value until required" in {
      val counter = new AtomicInteger(0)

      def value = {
        counter.incrementAndGet()
        "hello"
      }

      val hello = Lazy(value)
      hello.created() shouldBe false
      counter.get shouldBe 0
      hello.value shouldBe "hello"
      hello.created() shouldBe true
      hello.value shouldBe "hello"
      counter.get shouldBe 1

      hello.close()
    }
  }
}
