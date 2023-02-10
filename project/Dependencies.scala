import sbt._

object Dependencies {
  def scalaReflect(version: String) = "org.scala-lang" % "scala-reflect" % version % Provided

  /** Used in Akka file watcher */
  def akka(scalaVersion: Option[(Int, Int)]) = {
    val version = scalaVersion match {
      case Some((2, 11)) => "2.5.32"
      case _             => "2.7.0"
    }
    "com.typesafe.akka" %% "akka-actor" % version
  }

  // Test dependencies
  val scalatest = "org.scalatest" %% "scalatest"  % "3.2.15" % Test
  val shapeless = "com.chuusai"   %% "shapeless"  % "2.3.4"  % Test // For shapeless based Reader/Scanner in tests
  val commonsio = "commons-io"     % "commons-io" % "2.11.0" % Test // Benchmarks
  val fastjavaio =
    "fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar" // Benchmarks
}
