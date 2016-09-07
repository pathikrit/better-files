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
      case v if scalaVersion.startsWith("2.12") => artifact % "2.4.8"
      case v if scalaVersion.startsWith("2.11") => artifact % "2.4.8"
      case v if scalaVersion.startsWith("2.10") => artifact % "2.3.15"
    }
  }

  def shapeless(scalaVersion: String): ModuleID = {
    val artifact = "com.chuusai" %% "shapeless"
    if (scalaVersion.startsWith("2.12")) artifact % "2.3.2"
    else artifact % "2.3.1"
  }

  def scalaMeter(scalaVersion: String): Seq[ModuleID] =
    if (scalaVersion.startsWith("2.12")) Nil // no 2.12 support yet
    else Seq("com.storm-enroute" %% "scalameter-core" % "0.7" % Test)
}