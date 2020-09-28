val username = "pathikrit"
val repo     = "better-files"

inThisBuild(
  List(
    organization := "better.files",
    homepage := Some(url(s"https://github.com/$username/$repo")),
    licenses := List("MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE")),
    developers := List(
      Developer(
        id = username,
        name = "Pathikrit Bhowmick",
        email = "pathikritbhowmick@msn.com",
        url = new URL(s"http://github.com/${username}")
      )
    )
  )
)

lazy val commonSettings = Seq(
  organization := s"com.github.$username",
  scalaVersion := crossScalaVersions.value.find(_.startsWith("2.12")).get,
  crossScalaVersions := Seq("2.11.12", "2.12.12", "2.13.3"),
  crossVersion := CrossVersion.binary,
  scalacOptions := myScalacOptions(scalaVersion.value, scalacOptions.value),
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
def myScalacOptions(scalaVersion: String, suggestedOptions: Seq[String]): Seq[String] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 10)) => suggestedOptions diff Seq("-Ywarn-numeric-widen") // buggy in 2.10
    case Some((2, 11)) => suggestedOptions diff Seq("-Ywarn-value-discard") // This is broken in 2.11 for Unit types
    case Some((2, 13)) => Nil                                               // Ignore warnings for 2.13 for now
    case _             => Nil
  }

lazy val core = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := repo,
    description := "Simple, safe and intuitive I/O in Scala",
    libraryDependencies ++= Seq(
      Dependencies.scalaReflect(scalaVersion.value),
      Dependencies.commonsio,
      Dependencies.fastjavaio,
      Dependencies.shapeless
    )
  )

lazy val akka = (project in file("akka"))
  .settings(commonSettings: _*)
  .settings(
    name := s"$repo-akka",
    description := "Reactive file watcher using Akka actors",
    libraryDependencies += Dependencies.akka
  )
  .dependsOn(core % "test->test;compile->compile")

lazy val root = (project in file("."))
  .settings(name := s"$repo-root")
  .settings(commonSettings: _*)
  .settings(docSettings: _*)
  .settings(skip in publish := true)
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(GhpagesPlugin)
  .aggregate(core, akka)

lazy val docSettings = Seq(
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, akka),
  siteSourceDirectory := baseDirectory.value / "site",
  siteSubdirName in ScalaUnidoc := "latest/api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
  git.remoteRepo := s"git@github.com:$username/$repo.git",
  envVars in ghpagesPushSite += ("SBT_GHPAGES_COMMIT_MESSAGE" -> s"Publishing Scaladoc [CI SKIP]")
)

lazy val formatAll   = taskKey[Unit]("Format all the source code which includes src, test, and build files")
lazy val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")
