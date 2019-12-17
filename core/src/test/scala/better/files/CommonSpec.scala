package better.files

import org.scalatest._
import org.scalatest.flatspec._

import scala.concurrent.duration._
import scala.util.Properties._
import org.scalatest.matchers.should.Matchers

trait CommonSpec extends AnyFlatSpec with BeforeAndAfterEach with Matchers {
  def isLinux = osName.startsWith("Linux")

  val isCI = sys.env.get("CI").exists(_.toBoolean)

  val isUnixOS = isLinux || isMac

  val scalaVersion = versionNumberString

  def sleep(t: FiniteDuration = 2.second) = Thread.sleep(t.toMillis)
}
