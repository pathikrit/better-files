name := "better-files"
version := "2.10.0-SNAPSHOT"
description := "Scala wrapper for Java files"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
organization := "com.github.pathikrit"

scalaVersion := "2.11.7"
crossScalaVersions := Seq("2.10.5", "2.11.7")
crossVersion := CrossVersion.binary

javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
scalacOptions ++= Seq(  //copied from https://tpolecat.github.io/2014/04/11/scalac-flags.html
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  //"-Ywarn-value-discard",
  //"-Ywarn-unused-import",     // 2.11 only
  "-Xfuture"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka"   %% "akka-actor"    % "2.3.14",
  "org.scalatest"       %% "scalatest"     % "2.2.5"    % Test
)

site.settings
ghpages.settings
git.remoteRepo := "git@github.com:pathikrit/better-files.git"
site.includeScaladoc()
