val username = "pathikrit"
val repo = "better-files"

lazy val commonSettings = Seq(
  organization := s"com.github.$username",
  scalaVersion := "2.12.2",
  crossScalaVersions := Seq("2.12.2", "2.13.0-M1"),
  crossVersion := CrossVersion.binary,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
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
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Ywarn-unused-import",
    "-Ywarn-unused",
    "-Xexperimental",
    "-Xfuture"
  ),
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.2" % Test,
  updateImpactOpenBrowser := false
)

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
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.0"
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val shapelessScanner = (project in file("shapeless"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := s"shapeless-scanner",
    description := "Shapeless Scanner",
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2"
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val benchmarks = (project in file("benchmarks"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := s"$repo-benchmarks"
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(docSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(releaseSettings: _*)
  .aggregate(core, akka, shapelessScanner)

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
        <id>{username}</id>
        <name>Pathikrit Bhowmick</name>
        <url>http://github.com/{username}</url>
      </developer>
    </developers>
)
