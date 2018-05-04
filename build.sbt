val username = "pathikrit"
val repo     = "better-files"

val formatAll   = taskKey[Unit]("Format all the source code which includes src, test, and build files")
val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")

lazy val commonSettings = Seq(
  organization := s"com.github.$username",
  scalaVersion := "2.12.5",
  crossScalaVersions := Seq("2.11.12", "2.12.5"),
  crossVersion := CrossVersion.binary,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  scalacOptions --= ignoreScalacOptions(scalaVersion.value),
  scalacOptions in (Compile, doc) += "-groups",
  libraryDependencies += Dependencies.scalatest,
  updateImpactOpenBrowser := false,
  compile in Compile := (compile in Compile).dependsOn(formatAll).value,
  test in Test := (test in Test).dependsOn(checkFormat).value,
  formatAll := {
    (scalafmt in Compile).value
    (scalafmt in Test).value
    (scalafmtSbt in Compile).value
  },
  checkFormat := {
    (scalafmtCheck in Compile).value
    (scalafmtCheck in Test).value
    (scalafmtSbtCheck in Compile).value
  }
)

/** We use https://github.com/DavidGregory084/sbt-tpolecat but some of these are broken */
def ignoreScalacOptions(scalaVersion: String): Seq[String] = CrossVersion.partialVersion(scalaVersion) match {
  case Some((2, 10)) => Seq("-Ywarn-numeric-widen") // buggy in 2.10
  case Some((2, 11)) => Seq("-Ywarn-value-discard") // This is broken in 2.11 for Unit types
  case _             => Nil
}

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := repo,
    description := "Simple, safe and intuitive I/O in Scala",
    libraryDependencies += Dependencies.scalaReflect(scalaVersion.value) % "provided,optional",
    libraryDependencies ++= Dependencies.silencer
  )

lazy val akka = (project in file("akka"))
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(
    name := s"$repo-akka",
    description := "Reactive file watcher using Akka actors",
    libraryDependencies += Dependencies.akka
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val shapelessScanner = (project in file("shapeless"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := s"shapeless-scanner",
    description := "Shapeless Scanner",
    libraryDependencies += Dependencies.shapeless
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val benchmarks = (project in file("benchmarks"))
  .settings(commonSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    name := s"$repo-benchmarks",
    libraryDependencies ++= Seq(
      Dependencies.commonsio,
      Dependencies.fastjavaio
    )
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val root = (project in file("."))
  .settings(name := s"$repo-root")
  .settings(commonSettings: _*)
  .settings(docSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(releaseSettings: _*)
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(GhpagesPlugin)
  .aggregate(core, akka, shapelessScanner, benchmarks)

lazy val docSettings = Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, akka),
  siteSourceDirectory := baseDirectory.value / "site",
  siteSubdirName in ScalaUnidoc := "latest/api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
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
    releaseStepCommand("publishSigned"),
    setNextVersion,
    commitNextVersion,
    releaseStepCommand("sonatypeReleaseAll"),
    pushChanges
  )
)

lazy val noPublishSettings = Seq(
  publish := ((): Unit),
  publishLocal := ((): Unit),
  publishArtifact := false
)

lazy val publishSettings = Seq(
  homepage := Some(url(s"https://github.com/$username/$repo")),
  licenses += "MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE"),
  scmInfo := Some(ScmInfo(url(s"https://github.com/$username/$repo"), s"git@github.com:$username/$repo.git")),
  apiURL := Some(url(s"https://$username.github.io/$repo/latest/api/")),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  developers := List(
    Developer(id = username,
              name = "Pathikrit Bhowmick",
              email = "pathikritbhowmick@msn.com",
              url = new URL(s"http://github.com/${username}"))
  ),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := Some(if (isSnapshot.value) Opts.resolver.sonatypeSnapshots else Opts.resolver.sonatypeStaging),
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)).toSeq
)
