package better.files

import java.io._
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter
import java.util.StringTokenizer

trait Scanner extends Iterator[String] with AutoCloseable {
  def lineNumber(): Int

  def next[A](implicit scan: Scannable[A]): A = scan(this)

  def nextLine(): String

  def lines: Iterator[String] = Iterator.continually(nextLine()).withHasNext(hasNext)
}

/** Faster, safer and more idiomatic Scala replacement for java.util.Scanner
  * See: http://codeforces.com/blog/entry/7018
  */
object Scanner {
  val stdin: Scanner = Scanner(System.in)

  trait Source[A] { self =>
    def apply(a: A): LineNumberReader
    def contramap[B](f: B => A): Source[B] =
      new Source[B] {
        override def apply(b: B) = self.apply(f(b))
      }
  }

  object Source {
    def apply[A](f: A => LineNumberReader): Source[A] =
      new Source[A] {
        override def apply(a: A) = f(a)
      }

    trait Implicits {
      implicit val lineNumberReaderSource: Source[LineNumberReader] = Source(identity)
      implicit val bufferedReaderSource: Source[BufferedReader] =
        lineNumberReaderSource.contramap(new LineNumberReader(_))
      implicit val readerSource: Source[Reader] = bufferedReaderSource.contramap(_.buffered)
      implicit val stringSource: Source[String] = readerSource.contramap(new StringReader(_))

      implicit def inputstreamSource(implicit charset: Charset = DefaultCharset): Source[InputStream] =
        readerSource.contramap(_.reader(charset))
    }
  }

  def apply[A: Source](a: A, splitter: StringSplitter = StringSplitter.Default): Scanner =
    new Scanner {
      private[this] val reader  = implicitly[Source[A]].apply(a)
      private[this] val tokens  = reader.tokens(splitter)
      override def lineNumber() = reader.getLineNumber
      override def nextLine()   = Option(reader.readLine()).getOrElse(throw new NoSuchElementException("End of file"))
      override def next()       = tokens.next()
      override def hasNext      = tokens.hasNext
      override def close()      = reader.close()
    }

  trait Read[A] { // TODO: Move to own subproject when this is fixed https://github.com/typelevel/cats/issues/932
    def apply(s: String): A
  }

  object Read {
    def apply[A](f: String => A): Read[A] =
      new Read[A] {
        override def apply(s: String) = f(s)
      }

    trait Implicits {
      implicit val stringRead: Read[String]             = Read(identity)
      implicit val booleanRead: Read[Boolean]           = Read(_.toBoolean)
      implicit val byteRead: Read[Byte]                 = Read(_.toByte) // TODO: https://issues.scala-lang.org/browse/SI-9706
      implicit val shortRead: Read[Short]               = Read(_.toShort)
      implicit val intRead: Read[Int]                   = Read(_.toInt)
      implicit val longRead: Read[Long]                 = Read(_.toLong)
      implicit val bigIntRead: Read[BigInt]             = Read(BigInt(_))
      implicit val floatRead: Read[Float]               = Read(_.toFloat)
      implicit val doubleRead: Read[Double]             = Read(_.toDouble)
      implicit val bigDecimalRead: Read[BigDecimal]     = Read(BigDecimal(_))
      implicit def optionRead[A: Read]: Read[Option[A]] = Read(s => when(s.nonEmpty)(implicitly[Read[A]].apply(s)))

      // Java's time readers
      import java.time._
      import java.sql.{Date => SqlDate, Time => SqlTime, Timestamp => SqlTimestamp}

      implicit val durationRead: Read[Duration]             = Read(Duration.parse(_))
      implicit val instantRead: Read[Instant]               = Read(Instant.parse(_))
      implicit val localDateTimeRead: Read[LocalDateTime]   = Read(LocalDateTime.parse(_))
      implicit val localDateRead: Read[LocalDate]           = Read(LocalDate.parse(_))
      implicit val monthDayRead: Read[MonthDay]             = Read(MonthDay.parse(_))
      implicit val offsetDateTimeRead: Read[OffsetDateTime] = Read(OffsetDateTime.parse(_))
      implicit val offsetTimeRead: Read[OffsetTime]         = Read(OffsetTime.parse(_))
      implicit val periodRead: Read[Period]                 = Read(Period.parse(_))
      implicit val yearRead: Read[Year]                     = Read(Year.parse(_))
      implicit val yearMonthRead: Read[YearMonth]           = Read(YearMonth.parse(_))
      implicit val zonedDateTimeRead: Read[ZonedDateTime]   = Read(ZonedDateTime.parse(_))
      implicit val sqlDateRead: Read[SqlDate]               = Read(SqlDate.valueOf)
      implicit val sqlTimeRead: Read[SqlTime]               = Read(SqlTime.valueOf)
      implicit val sqlTimestampRead: Read[SqlTimestamp]     = Read(SqlTimestamp.valueOf)

      /** Use this to create custom readers e.g. to read a LocalDate using some custom format
        * val readLocalDate: Read[LocalDate] = Read.temporalQuery(format = myFormat, query = LocalDate.from)
        */
      def temporalQuery[A](format: DateTimeFormatter, query: temporal.TemporalQuery[A]): Read[A] =
        Read(format.parse(_, query))
    }
  }
}

/** Implement this trait to make thing parsable
  * In most cases, use Scanner.Read typeclass when you simply need access to one String token
  * Use Scannable typeclass if you need access to the full scanner e.g. to detect encodings etc.
  */
trait Scannable[A] {
  def apply(scanner: Scanner): A
}

object Scannable {
  def apply[A](f: Scanner => A): Scannable[A] =
    new Scannable[A] {
      override def apply(scanner: Scanner) = f(scanner)
    }

  implicit def fromRead[A](implicit read: Scanner.Read[A]): Scannable[A] =
    Scannable(s => read(s.next()))

  implicit def tuple2[T1, T2](implicit t1: Scannable[T1], t2: Scannable[T2]): Scannable[(T1, T2)] =
    Scannable(s => t1(s) -> t2(s))

  implicit def iterator[A](implicit scanner: Scannable[A]): Scannable[Iterator[A]] =
    Scannable(s => Iterator.continually(scanner(s)).withHasNext(s.hasNext))
}

trait StringSplitter {
  def split(s: String): TraversableOnce[String]
}
object StringSplitter {
  val Default = StringSplitter.anyOf(" \t\t\n\r")

  /** Split string on this character
    * This will return exactly 1 + n number of items where n is the number of occurrence of delimiter in String s
    */
  def on(delimiter: Char): StringSplitter =
    new StringSplitter {
      override def split(s: String) =
        new Iterator[String] {
          private[this] var i = 0
          private[this] var j = -1
          private[this] val c = delimiter.toInt
          _next()

          private[this] def _next() = {
            i = j + 1
            val k = s.indexOf(c, i)
            j = if (k < 0) s.length else k
          }

          override def hasNext = i <= s.length

          override def next() = {
            val res = s.substring(i, j)
            _next()
            res
          }
        }
    }

  /** Split this string using ANY of the characters from delimiters */
  def anyOf(delimiters: String, includeDelimiters: Boolean = false): StringSplitter =
    new StringSplitter {
      override def split(s: String) = new StringTokenizer(s, delimiters, includeDelimiters)
    }

  /** Split string using a regex pattern */
  def regex(pattern: String): StringSplitter =
    new StringSplitter {
      override def split(s: String) = s.split(pattern, -1)
    }
}
