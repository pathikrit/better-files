name := "better-files"
version := "2.4.1-SNAPSHOT"
description := "Scala wrapper for Java files"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
organization := "com.github.pathikrit"

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.5", "2.11.7")
crossVersion := CrossVersion.binary

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature",
  "-language:implicitConversions", "-language:postfixOps"
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % Test
