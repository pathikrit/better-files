package better.files

import Dsl._

import scala.language.existentials

class ScannerSpec extends CommonSpec {
  def t1 = File.newTemporaryFile()

  "scanner" should "parse files" in {
    val data = t1 << s"""
    | Hello World
    | 1 2 3
    | Ok 23 football
    """.stripMargin
    val scanner: Scanner = data.newScanner()
    assert(scanner.lineNumber() == 0)
    assert(scanner.next[String] == "Hello")
    assert(scanner.lineNumber() == 2)
    assert(scanner.next[String] == "World")
    assert(scanner.next[Int] == 1)
    assert(scanner.next[Int] == 2)
    assert(scanner.lineNumber() == 3)
    assert(scanner.next[Int] == 3)
    assert(scanner.next[String] == "Ok")
    assert(scanner.tillEndOfLine() == " 23 football")
    assert(!scanner.hasNext)
    a[NoSuchElementException] should be thrownBy scanner.tillEndOfLine()
    a[NoSuchElementException] should be thrownBy scanner.next()
    assert(!scanner.hasNext)
    data.lineIterator.toSeq.filterNot(_.trim.isEmpty) shouldEqual data.newScanner.nonEmptyLines.toSeq
    data.tokens.toSeq shouldEqual data.newScanner().toSeq
  }

  it should "parse longs/booleans" in {
    val data = for {
      scanner <- Scanner("10 false").autoClosed
    } yield scanner.next[(Long, Boolean)]
    data shouldBe ((10L, false))
  }

  it should "parse custom parsers" in {
    val file = t1 < """
      |Garfield
      |Woofer
    """.stripMargin

    sealed trait Animal
    case class Dog(name: String) extends Animal
    case class Cat(name: String) extends Animal

    implicit val animalParser: Scannable[Animal] = Scannable {scanner =>
      val name = scanner.next[String]
      if (name == "Garfield") Cat(name) else Dog(name)
    }
    val scanner = file.newScanner()
    Seq.fill(2)(scanner.next[Animal]) should contain theSameElementsInOrderAs Seq(Cat("Garfield"), Dog("Woofer"))
  }
}
