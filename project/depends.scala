import sbt._

object depends {

  def scalatest(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) "org.scalatest" %% "scalatest" % "3.0.0" % Test
    else "org.scalatest" %% "scalatest" % "2.2.6" % Test

  def akkaActor(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) "com.typesafe.akka" %% "akka-actor" % "2.4.8"
    else if (scalaVersion.startsWith("2.11")) "com.typesafe.akka" %% "akka-actor" % "2.4.8"
    else if (scalaVersion.startsWith("2.10")) "com.typesafe.akka" %% "akka-actor" % "2.3.15"
    else ???

  def shapeless(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) "com.chuusai" %% "shapeless" % "2.3.2"
    else "com.chuusai" %% "shapeless" % "2.3.1"

  def scalaMeter(scalaVersion: String) =
    if (scalaVersion.startsWith("2.12")) Nil // no 2.12 support yet
    else Seq("com.storm-enroute" %% "scalameter-core" % "0.7" % Test)
}