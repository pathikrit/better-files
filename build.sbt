lazy val commonSettings = Seq(
  version := "2.12.2-SNAPSHOT",
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT"),
  organization := "com.github.pathikrit",
  homepage := Some(url("https://github.com/pathikrit/better-files")),
  scmInfo := Some(ScmInfo(url("https://github.com/pathikrit/better-files"), "git@github.com:pathikrit/better-files.git")),
  /*developers := List(
    Developer("pathikrit", "Pathikrit Bhowmick", "pathikritbhowmick@msn.com", url("https://github.com/pathikrit"))
  )*/
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),
  crossVersion := CrossVersion.binary,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
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
    //"-Ywarn-numeric-widen",     // issue in 2.10
    //"-Ywarn-value-discard",
    //"-Ywarn-unused-import",     // 2.11 only
    "-Xfuture"
  ),
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % Test
)

lazy val core = (project in file("core"))
  .settings(commonSettings : _*)
  .settings(
    name := "better-files",
    description := "Simple, safe and intuitive I/O in Scala"
  )

lazy val akka = (project in file("akka"))
  .settings(commonSettings : _*)
  .settings(
    name := "better-files-akka",
    description := "Reactive file watchers",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  )
  .dependsOn(core)

site.settings
ghpages.settings
git.remoteRepo := "git@github.com:pathikrit/better-files.git"
site.includeScaladoc()
