import sbt._

object DependsOn {

  def scalatest(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) "org.scalatest" %% "scalatest" % "3.0.1" % Test
    else "org.scalatest" %% "scalatest" % "2.2.6" % Test

  def akkaActor(scalaVersion: String) =
    scalaVersion match {
      case v if scalaVersion.startsWith("2.12") => "com.typesafe.akka" %% "akka-actor" % "2.4.14"
      case v if scalaVersion.startsWith("2.11") => "com.typesafe.akka" %% "akka-actor" % "2.4.8"
      case v if scalaVersion.startsWith("2.10") => "com.typesafe.akka" %% "akka-actor" % "2.3.15"
    }

  val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"

  def scalaMeter(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) "com.storm-enroute" %% "scalameter-core" % "0.8.1" % Test
    else "com.storm-enroute" %% "scalameter-core" % "0.7" % Test
}