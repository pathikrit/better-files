import sbt._

object Dependencies {

  /** Used in Akka file watcher */
  def akka(scalaVersion: String) = {
    val version = if (scalaVersion.startsWith("2.11")) "2.5.32" else "2.7.0"
    "com.typesafe.akka" %% "akka-actor" % version
  }

  def testDependencies(scalaVersion: String) =
    Seq(
      "org.scalatest" %% "scalatest"  % "3.2.15" % Test,
      "commons-io"     % "commons-io" % "2.11.0" % Test,
      "fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar" // Benchmarks
    ) ++ (if (scalaVersion.startsWith("2."))
            Seq(
              "com.chuusai"   %% "shapeless"     % "2.3.4"      % Test, // For shapeless based Reader/Scanner in tests
              "org.scala-lang" % "scala-reflect" % scalaVersion % Provided
            )
          else Nil)
}
