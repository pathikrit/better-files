val username = "pathikrit"
val repo     = "better-files"

inThisBuild(
  List(
    organization.withRank(KeyRanks.Invisible) := "better.files", // TODO: repo.replace("-", ".")
    homepage                                  := Some(url(s"https://github.com/$username/$repo")),
    licenses                                  := List("MIT" -> url(s"https://github.com/$username/$repo/blob/master/LICENSE")),
    developers := List(
      Developer(
        id = username,
        name = "Pathikrit Bhowmick",
        email = "pathikritbhowmick@msn.com",
        url = new URL(s"http://github.com/$username")
      )
    ),
    Global / onChangedBuildSource := ReloadOnSourceChanges
  )
)

lazy val main = (project in file("."))
  .settings(
    // Names
    name         := repo,
    description  := "Simple, safe and intuitive I/O in Scala",
    organization := s"com.github.$username",

    // scalac versions
    scalaVersion       := crossScalaVersions.value.find(_.startsWith("2.12")).get,
    crossScalaVersions := Seq("2.11.12", "2.12.17", "2.13.10", "3.2.2"),
    crossVersion       := CrossVersion.binary,

    // Compile settings
    scalacOptions := myScalacOptions(scalaVersion.value, scalacOptions.value),
    Compile / doc / scalacOptions += "-groups",
    Compile / compile := (Compile / compile).dependsOn(formatAll).value,
    formatAll := {
      (Compile / scalafmt).value
      (Test / scalafmt).value
      (Compile / scalafmtSbt).value
    },

    // Test settings
    Test / testOptions += Tests.Argument("-oDF"), // show full stack trace on test failures
    Test / test := (Test / test).dependsOn(checkFormat).value,
    checkFormat := {
      (Compile / scalafmtCheck).value
      (Test / scalafmtCheck).value
      (Compile / scalafmtSbtCheck).value
    },

    // Dependencies
    libraryDependencies ++= Dependencies.testDependencies(scalaVersion.value)
  )

/** We use https://github.com/DavidGregory084/sbt-tpolecat but some of these are broken */
def myScalacOptions(scalaVersion: String, suggestedOptions: Seq[String]): Seq[String] =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, 10)) => suggestedOptions diff Seq("-Ywarn-numeric-widen") // buggy in 2.10
    case Some((2, 11)) => suggestedOptions diff Seq("-Ywarn-value-discard") // This is broken in 2.11 for Unit types
    case Some((2, 12)) => suggestedOptions
    case _             => Nil
  }

lazy val docSettings = Seq(
  autoAPIMappings                            := true,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(main),
  siteSourceDirectory                        := baseDirectory.value / "site",
  ScalaUnidoc / siteSubdirName               := "latest/api",
  addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, ScalaUnidoc / siteSubdirName),
  git.remoteRepo                                             := s"git@github.com:$username/$repo.git",
  ghpagesPushSite / envVars += ("SBT_GHPAGES_COMMIT_MESSAGE" -> s"Publishing Scaladoc [CI SKIP]")
)

lazy val formatAll   = taskKey[Unit]("Format all the source code which includes src, test, and build files")
lazy val checkFormat = taskKey[Unit]("Check all the source code which includes src, test, and build files")
