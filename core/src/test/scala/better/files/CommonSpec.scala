package better.files

import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps

trait CommonSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  val isCI = sys.env.get("CI").exists(_.toBoolean)

  val isUnixOS = sys.props.get("os.name") match {
    case Some("Linux" | "MaxOS") => true
    case _ => false
  }

  def sleep(t: FiniteDuration = 2 second) = Thread.sleep(t.toMillis)
}