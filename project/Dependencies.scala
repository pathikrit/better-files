import sbt._

object Dependencies {
  val scalatest = "org.scalatest" %% "scalatest" % "3.0.5" % Test

  // Used in Akka file watcher
  val akka = "com.typesafe.akka" %% "akka-actor" % "2.5.12"

  // For shapeless based Reader/Scanner
  val shapeless = "com.chuusai" %% "shapeless" % "2.3.3"

  // Used in Benchmarks only
  val commonsio  = "commons-io" % "commons-io" % "2.6"
  val fastjavaio = "fastjavaio" % "fastjavaio" % "1.0" from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar"

  def scalaReflect(version: String) = "org.scala-lang" % "scala-reflect" % version % "provided,optional"
}
