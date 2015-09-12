name := "better-files"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.7"

description := "Yet another Scala lens macro"

licenses +=("MIT", url("http://opensource.org/licenses/MIT"))

organization := "com.github.pathikrit"

crossScalaVersions := Seq("2.10.3", "2.10.4", "2.10.5", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-language:experimental.macros")

resolvers += Resolver.typesafeRepo("releases")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % Test

seq(bintraySettings: _*)
