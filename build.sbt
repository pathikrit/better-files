import sbt._
import Keys._
import com.typesafe.sbt.SbtSite._


val username = "pathikrit"
val repo = "better-files"


lazy val commonSettings = Seq(
  organization := s"com.github.$username",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1"),
  crossVersion := CrossVersion.binary,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  unmanagedSourceDirectories in Test ++=
    Seq((sourceDirectory in Test).value / s"scala-${scalaBinaryVersion.value}"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Xfuture"
  ) ++ compilerOptionsFor(scalaVersion.value),
  libraryDependencies += DependsOn.scalatest(scalaVersion.value),
  updateImpactOpenBrowser := false
)

def compilerOptionsFor(scalaVersion: String) =
  if (scalaVersion.startsWith("2.10")) Seq()
  else if (scalaVersion.startsWith("2.11")) Seq("-Yinline-warnings", "-Ywarn-unused-import", "-Ywarn-unused", "-Xexperimental", "-Ywarn-numeric-widen")
  else if (scalaVersion.startsWith("2.12")) Seq("-Ywarn-unused-import", "-Ywarn-unused", "-Xexperimental", /*"-Ywarn-value-discard", */"-Ywarn-numeric-widen")
  else Nil


lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := repo,
    description := "Simple, safe and intuitive I/O in Scala"
  )

lazy val akka = (project in file("akka"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := s"$repo-akka",
    description := "Reactive file watcher using Akka actors",
    libraryDependencies += DependsOn.akkaActor(scalaVersion.value)
  )
  .dependsOn(core)

lazy val shapelessScanner = (project in file("shapeless"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := s"shapeless-scanner",
    description := "Shapeless Scanner",
    libraryDependencies += DependsOn.shapeless(scalaVersion.value)
  )
  .dependsOn(core)

lazy val benchmarks = Project(id = "benchmarks", base = file("benchmarks"),
  settings = commonSettings ++
             Seq(libraryDependencies += DependsOn.scalaMeter(scalaVersion.value)) ++
             Seq(name := s"$repo-benchmarks") ++ noPublishSettings ++
             Seq((skip in compile) := scalaVersion.value startsWith "2.12")
)
  .dependsOn(core)

lazy val root =
  Project(
    id = "better-files",
    base = file( "." ),
    settings = Seq(name:= "Better Files") ++ commonSettings ++ docSettings ++ noPublishSettings ++ releaseSettings
  ).aggregate(core, akka, shapelessScanner)

import UnidocKeys._
lazy val docSettings = unidocSettings ++ site.settings ++ ghpages.settings ++ Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, akka),
  SiteKeys.siteSourceDirectory := file("site"),
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "latest/api"),
  git.remoteRepo := s"git@github.com:$username/$repo.git"
)

import ReleaseTransformations._
lazy val releaseSettings = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    //runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  apiURL := Some(url(s"https://$username.github.io/$repo/latest/api/")),
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
