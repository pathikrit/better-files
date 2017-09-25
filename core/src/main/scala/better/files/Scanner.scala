package better.files

import java.io.{BufferedReader, InputStream, LineNumberReader, Reader, StringReader}
import java.nio.charset.Charset
import java.time.format.DateTimeFormatter

trait Scanner extends Iterator[String] with AutoCloseable {
  def lineNumber(): Int

  def next[A](implicit scan: Scannable[A]): A = scan(this)

  def tillDelimiter(delimiter: String): String

  def tillEndOfLine(): String = tillDelimiter(Scanner.Config.Delimiters.lines)

  def nonEmptyLines: Iterator[String] = Iterator.continually(tillEndOfLine()).withHasNext(hasNext)
}

/**
  * Faster, safer and more idiomatic Scala replacement for java.util.Scanner
  * See: http://codeforces.com/blog/entry/7018
  */
object Scanner {

  def apply(str: String)(implicit config: Config): Scanner =
    Scanner(new StringReader(str))(config)

  def apply(reader: Reader)(implicit config: Config): Scanner =
    Scanner(reader.buffered)(config)

  def apply(reader: BufferedReader)(implicit config: Config): Scanner =
    Scanner(new LineNumberReader(reader))(config)

  def apply(inputStream: InputStream)(implicit config: Config): Scanner =
    Scanner(inputStream.reader(config.charset))(config)

  def apply(reader: LineNumberReader)(implicit config: Config): Scanner = new Scanner {
    private[this] val tokenizers = reader.tokenizers(config).buffered
    private[this] def tokenizer() = {
      while (tokenizers.headOption.exists(st => !st.hasMoreTokens)) tokenizers.next()
      tokenizers.headOption
    }
    override def lineNumber() = reader.getLineNumber
    override def tillDelimiter(delimiter: String) = tokenizer().get.nextToken(delimiter)
    override def next() = tokenizer().get.nextToken()
    override def hasNext = tokenizer().nonEmpty
    override def close() = reader.close()
  }

  val stdin: Scanner = Scanner(System.in)(Config.default)

  /**
    * Use this to configure your Scanner
    *
    * @param delimiter
    * @param includeDelimiters
    */
  case class Config(delimiter: String, includeDelimiters: Boolean)(implicit val charset: Charset = defaultCharset)
  object Config {
    implicit val default = Config(delimiter = Delimiters.whitespaces, includeDelimiters = false)

    object Delimiters {
      val lines = "\n\r"
      val whitespaces = " \t\f" + lines
    }
  }

  trait Read[A] {     // TODO: Move to own subproject when this is fixed https://github.com/typelevel/cats/issues/932
    def apply(s: String): A
  }

  object Read {
    def apply[A](f: String => A): Read[A] = new Read[A] {
      override def apply(s: String) = f(s)
    }
    implicit val string           : Read[String]            = Read(identity)
    implicit val boolean          : Read[Boolean]           = Read(_.toBoolean)
    implicit val byte             : Read[Byte]              = Read(_.toByte)  //TODO: https://issues.scala-lang.org/browse/SI-9706
    implicit val short            : Read[Short]             = Read(_.toShort)
    implicit val int              : Read[Int]               = Read(_.toInt)
    implicit val long             : Read[Long]              = Read(_.toLong)
    implicit val bigInt           : Read[BigInt]            = Read(BigInt(_))
    implicit val float            : Read[Float]             = Read(_.toFloat)
    implicit val double           : Read[Double]            = Read(_.toDouble)
    implicit val bigDecimal       : Read[BigDecimal]        = Read(BigDecimal(_))
    implicit def option[A: Read]  : Read[Option[A]]         = Read(s => when(s.nonEmpty)(implicitly[Read[A]].apply(s)))

    // Java's time readers
    import java.time._
    import java.sql.{Date => SqlDate, Time => SqlTime, Timestamp => SqlTimestamp}

    implicit val duration         : Read[Duration]          = Read(Duration.parse(_))
    implicit val instant          : Read[Instant]           = Read(Instant.parse(_))
    implicit val localDateTime    : Read[LocalDateTime]     = Read(LocalDateTime.parse(_))
    implicit val localDate        : Read[LocalDate]         = Read(LocalDate.parse(_))
    implicit val monthDay         : Read[MonthDay]          = Read(MonthDay.parse(_))
    implicit val offsetDateTime   : Read[OffsetDateTime]    = Read(OffsetDateTime.parse(_))
    implicit val offsetTime       : Read[OffsetTime]        = Read(OffsetTime.parse(_))
    implicit val period           : Read[Period]            = Read(Period.parse(_))
    implicit val year             : Read[Year]              = Read(Year.parse(_))
    implicit val yearMonth        : Read[YearMonth]         = Read(YearMonth.parse(_))
    implicit val zonedDateTime    : Read[ZonedDateTime]     = Read(ZonedDateTime.parse(_))
    implicit val sqlDate          : Read[SqlDate]           = Read(SqlDate.valueOf)
    implicit val sqlTime          : Read[SqlTime]           = Read(SqlTime.valueOf)
    implicit val sqlTimestamp     : Read[SqlTimestamp]      = Read(SqlTimestamp.valueOf)

    /**
      * Use this to create custom readers e.g. to read a LocalDate using some custom format
      * val readLocalDate: Read[LocalDate] = Read.temporalQuery(format = myFormat, query = LocalDate.from)
      * @param format
      * @param query
      * @tparam A
      * @return
      */
    def temporalQuery[A](format: DateTimeFormatter, query: temporal.TemporalQuery[A]): Read[A] =
      Read(format.parse(_, query))
  }
}

/**
  * Implement this trait to make thing parsable
  * In most cases, use Scanner.Read typeclass when you simply need access to one String token
  * Use Scannable typeclass if you need access to the full scanner e.g. to detect encodings etc.
  */
trait Scannable[A] {
  def apply(scanner: Scanner): A
}

object Scannable {
  def apply[A](f: Scanner => A): Scannable[A] = new Scannable[A] {
    override def apply(scanner: Scanner) = f(scanner)
  }

  implicit def fromRead[A](implicit read: Scanner.Read[A]): Scannable[A] =
    Scannable(s => read(s.next()))

  implicit def tuple2[T1, T2](implicit t1: Scannable[T1], t2: Scannable[T2]): Scannable[(T1, T2)] =
    Scannable(s => t1(s) -> t2(s))

  implicit def iterator[A](implicit scanner: Scannable[A]): Scannable[Iterator[A]] =
    Scannable(s => Iterator.continually(scanner(s)).withHasNext(s.hasNext))
}
