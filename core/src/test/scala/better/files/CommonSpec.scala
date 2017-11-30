package better.files

import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Properties.{osName, isMac, versionNumberString}

trait CommonSpec extends FlatSpec with BeforeAndAfterEach with Matchers {
  def isLinux = osName startsWith "Linux" //TODO: this is now in Scala:  https://github.com/scala/scala/commit/71f2bc737d96fcd29fcf2e5f494c6ae259d7b64e

  val isCI = sys.env.get("CI").exists(_.toBoolean)

  val isUnixOS = isLinux || isMac

  val scalaVersion = versionNumberString

  def sleep(t: FiniteDuration = 2 second) = Thread.sleep(t.toMillis)
}
