name := "better-files"
version := "0.0.1-SNAPSHOT"
description := "Scala wrapper for Java files"
licenses +=("MIT", url("http://opensource.org/licenses/MIT"))
organization := "com.github.pathikrit"

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.3", "2.10.4", "2.10.5", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7")
scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:implicitConversions")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % Test
