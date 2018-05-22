import sbt._

object Dependencies {
  def scalaReflect(version: String) = "org.scala-lang" % "scala-reflect" % version % "provided,optional"

  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

  // Used in Akka file watcher
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.12"

  // For shapeless based Reader/Scanner
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  // Benchmarks
  val commonsio  = "commons-io" % "commons-io" % "2.6" % Test
  val fastjavaio = "fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar"
}
