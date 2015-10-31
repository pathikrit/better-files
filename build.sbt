lazy val commonSettings = Seq(
  version := "2.13.0-SNAPSHOT",
  organization := "com.github.pathikrit",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7"),
  crossVersion := CrossVersion.binary,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yinline-warnings",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    //"-Ywarn-numeric-widen",     // bugs in 2.10
    //"-Ywarn-value-discard",
    //"-Ywarn-unused-import",     // 2.11 only
    //"-Xexperimental",           // 2.11 only
    "-Xfuture"
  ),
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % Test
)

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "better-files",
    description := "Simple, safe and intuitive I/O in Scala"
  )

lazy val akka = (project in file("akka"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := "better-files-akka",
    description := "Reactive file watcher using Akka actors",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.14"
  )
  .dependsOn(core)

lazy val benchmarks = (project in file("benchmarks"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := "better-files-benchmarks",
    libraryDependencies += "com.storm-enroute" %% "scalameter-core" % "0.7" % Test
  )
  .dependsOn(core)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(docSettings: _*)
  .settings(noPublishSettings: _*)
  .aggregate(core, akka)

import UnidocKeys._
lazy val docSettings = unidocSettings ++ site.settings ++ ghpages.settings ++ Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, akka),
  SiteKeys.siteSourceDirectory := file("site"),
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
  git.remoteRepo := "git@github.com:pathikrit/better-files.git"
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/pathikrit/better-files")),
  licenses += "MIT" -> url("http://opensource.org/licenses/MIT"),
  scmInfo := Some(ScmInfo(url("https://github.com/pathikrit/better-files"), "git@github.com:pathikrit/better-files.git")),
  apiURL := Some(url("https://pathikrit.github.io/better-files/latest/api/")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq,
  pomExtra :=
    <developers>
      <developer>
        <id>pathikrit</id>
        <name>Pathikrit Bhowmick</name>
        <url>http://github.com/pathikrit</url>
      </developer>
    </developers>
)
