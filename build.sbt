val username = "pathikrit"
val repo     = "better-files"

inThisBuild(
  List(
    organization.withRank(KeyRanks.Invisible) := repo.replace("-", "."),
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
    crossScalaVersions := Seq("2.11.12", "2.12.17", "2.13.10", "3.2.2"),
    crossVersion       := CrossVersion.binary,

    // Compile settings
    scalacOptions     := scalacOptions.value diff rmCompilerFlags(scalaVersion.value),
    Compile / compile := (Compile / compile).dependsOn(formatAll).value, // auto format on compile

    // Test settings
    Test / testOptions += Tests.Argument("-oDF"), // show full stack trace on test failures
    Test / test := (Test / test).dependsOn(checkFormat).value, // fail tests if code is not formatted

    // Dependencies
    libraryDependencies ++= myDependencies(scalaVersion.value),

    // Formatting tasks
    formatAll := {
      (Compile / scalafmt).value
      (Test / scalafmt).value
      (Compile / scalafmtSbt).value
    },
    checkFormat := {
      (Compile / scalafmtCheck).value
      (Test / scalafmtCheck).value
      (Compile / scalafmtSbtCheck).value
    }
  )
  // makeSite settings
  .enablePlugins(SiteScaladocPlugin, PreprocessPlugin)
  .settings(
    SiteScaladoc / siteSubdirName := "api/default",
    Preprocess / preprocessVars := Map(
      "scalaVersions" -> crossScalaVersions.value.map(partialVersion(_)).map(v => s"'$v'").mkString(", ")
    ),
    makeCrossSite := crossScalaVersions.value.foreach(copyDocs(_, destination = file("target/site")))
  )

// Useful formatting tasks
lazy val formatAll   = taskKey[Unit]("Format all the source (src, test, and build files)")
lazy val checkFormat = taskKey[Unit]("Check format for all the source (src, test, and build files)")

/** We use https://github.com/DavidGregory084/sbt-tpolecat but this gives us a way to remove some unruly flags */
def rmCompilerFlags(scalaVersion: String): Seq[String] =
  partialVersion(scalaVersion) match {
    case "2.13" | "3.2" => Seq("-Xfatal-warnings")
    case _              => Nil
  }

/** My dependencies - note this is a zero dependency library, so following are only for Tests */
def myDependencies(scalaVersion: String): Seq[ModuleID] =
  Seq(
    "2" -> ("org.scala-lang" % "scala-reflect" % scalaVersion % Provided),
    "2" -> ("com.chuusai"   %% "shapeless"     % "2.3.4"      % Test), // For shapeless based Reader/Scanner in tests
    "*" -> ("org.scalatest" %% "scalatest"     % "3.2.15"     % Test),
    "*" -> ("commons-io"     % "commons-io"    % "2.11.0"     % Test),
    "*" -> ("fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar"), // Benchmarks
    "*" -> ("com.typesafe.akka" %% "akka-actor" % (if (scalaVersion.startsWith("2.11")) "2.5.32" else "2.7.0") % Test)
  ).collect({ case (v, lib) if v == "*" || scalaVersion.startsWith(v) => lib })

// TODO: Remove below once https://github.com/sbt/sbt-site/issues/208 is fixed
lazy val makeCrossSite = taskKey[Unit]("Copy crossScalaVersion Scaladocs")

/** Util that copies scaladocs across scalaVersions + any static site sources into destination */
def copyDocs(scalaVersion: String, destination: File) =
  IO.copyDirectory(
    source = file("target") / s"scala-${partialVersion(scalaVersion, scalaVersion.head.toInt)}" / "api",
    target = destination / "api" / partialVersion(scalaVersion)
  )

def partialVersion(scalaVersion: String, n: Int = 2): String = scalaVersion.split('.').take(n).mkString(".")
