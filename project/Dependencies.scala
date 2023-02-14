import sbt._

object Dependencies {
  def testDependencies(scalaVersion: String) =
    Seq(
      "org.scalatest" %% "scalatest"  % "3.2.15" % Test,
      "commons-io"     % "commons-io" % "2.11.0" % Test,
      "fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar", // Benchmarks
      "com.typesafe.akka" %% "akka-actor" % (if (scalaVersion.startsWith("2.11")) "2.5.32" else "2.7.0")
    ) ++ (if (scalaVersion.startsWith("2."))
            Seq(
              "com.chuusai"   %% "shapeless"     % "2.3.4"      % Test, // For shapeless based Reader/Scanner in tests
              "org.scala-lang" % "scala-reflect" % scalaVersion % Provided
            )
          else Nil)
}
