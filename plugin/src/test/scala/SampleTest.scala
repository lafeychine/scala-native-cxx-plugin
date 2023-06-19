import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.*

class SampleTest extends AnyFunSuite:

    test("Proof of concept") {
        val compiler = DottyCompiler()

        val result = compiler.withPlugin("Foo.scala" -> """
            |@externCxx object Foo:
            |    type FooNative = CStruct2[CInt, CInt]
            |
            |    def ctor(): Unit = extern
        """)

        val expected = compiler.withoutPlugin("Foo.scala" -> """
            |@extern object Foo:
            |    type FooNative = CStruct2[CInt, CInt]
            |
            |    @name("_ZN3FooC2Ev")
            |    def ctor(): Unit = extern
        """)

        (expected zip result) foreach { (a, b) => assert(a == b) }
    }
