import sbt._

object DependsOn {

  def scalatest(scalaVersion: String): ModuleID = {
    val artifact = "org.scalatest" %% "scalatest"
    if (scalaVersion.startsWith("2.12")) artifact % "3.0.0" % Test
    else artifact % "2.2.6" % Test
  }

  def akkaActor(scalaVersion: String): ModuleID = {
    val artifact = "com.typesafe.akka" %% "akka-actor"
    scalaVersion match {
      case v if scalaVersion.startsWith("2.12") => artifact % "2.4.12"
      case v if scalaVersion.startsWith("2.11") => artifact % "2.4.8"
      case v if scalaVersion.startsWith("2.10") => artifact % "2.3.15"
    }
  }

  def shapeless(scalaVersion: String): ModuleID = {
    val artifact = "com.chuusai" %% "shapeless"
    if (scalaVersion.startsWith("2.12")) artifact % "2.3.2"
    else artifact % "2.3.1"
  }

  def scalaMeter(scalaVersion: String): ModuleID = {
    val artifact = "com.storm-enroute" %% "scalameter-core"
    if (scalaVersion.startsWith("2.12")) artifact % "0.8.1" % Test
    else artifact % "0.7" % Test
  }
}