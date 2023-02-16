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
    name               := repo,
    description        := "Simple, safe and intuitive I/O in Scala",
    crossScalaVersions := Seq("2.11.12", "2.12.17", "2.13.10", "3.2.2"),
    crossVersion       := CrossVersion.binary,
    scalacOptions      := scalacOptions.value diff rmCompilerFlags(scalaVersion.value),
    Compile / compile  := (Compile / compile).dependsOn(Compile / scalafmt, Test / scalafmt, Compile / scalafmtSbt).value,
    Test / test        := (Test / test).dependsOn(Compile / scalafmtCheck, Test / scalafmtCheck, Compile / scalafmtSbtCheck).value,
    Test / testOptions += Tests.Argument("-oDF"), // show full stack trace on test failures
    libraryDependencies ++= dependencies(scalaVersion.value)
  )
  // makeSite settings
  .enablePlugins(SiteScaladocPlugin, PreprocessPlugin)
  .settings(
    SiteScaladoc / siteSubdirName := "api/default",
    Preprocess / preprocessVars := Map(
      "scalaVersions" -> crossScalaVersions.value.map(CrossVersion.binaryScalaVersion).map(v => s"'$v'").mkString(", ")
    ),
    // See https://github.com/sbt/sbt/discussions/7151: Hack to make makeSite play well with crossScalaVersion
    makeSite := {
      val dest = makeSite.value
      IO.copyDirectory(source = (Compile / doc).value, target = dest / "api" / CrossVersion.binaryScalaVersion(scalaVersion.value))
      dest
    }
  )

/** We use https://github.com/DavidGregory084/sbt-tpolecat but this gives us a way to remove some unruly flags */
def rmCompilerFlags(scalaVersion: String): Seq[String] =
  CrossVersion.binaryScalaVersion(scalaVersion) match {
    case "2.11" | "2.12" => Seq("-Ywarn-value-discard")
    case "2.13"          => Seq("-Wvalue-discard", "-Wnonunit-statement", "-Wunused:imports")
    case _               => Nil
  }

/** My dependencies - note this is a zero dependency library, so following are only for Tests */
def dependencies(scalaVersion: String): Seq[ModuleID] =
  Seq(
    // TODO: Get rid of scala-collection-compat when we drop support for Scala 2.1 and -Wunused:imports since it triggers https://github.com/scala/scala-collection-compat/issues/240
    "*" -> "org.scala-lang.modules" %% "scala-collection-compat" % "2.9.0",
    "2" -> ("org.scala-lang"         % "scala-reflect"           % scalaVersion % Provided),
    "2" -> ("com.chuusai"           %% "shapeless"               % "2.3.4"      % Test), // For shapeless based Reader/Scanner in tests
    "*" -> ("org.scalatest"         %% "scalatest"               % "3.2.15"     % Test),
    "*" -> ("commons-io"             % "commons-io"              % "2.11.0"     % Test),
    "*" -> ("fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar"), // Benchmarks
    "*" -> ("com.typesafe.akka" %% "akka-actor" % (if (scalaVersion.startsWith("2.11")) "2.5.32" else "2.7.0") % Test)
  ).collect({ case (v, lib) if v == "*" || scalaVersion.startsWith(v) => lib })
