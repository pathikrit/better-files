import sbt._

object Dependencies {
  def scalaReflect(version: String) = "org.scala-lang"     % "scala-reflect" % version % Provided
  val akka                          = "com.typesafe.akka" %% "akka-actor"    % "2.5.31" // Used in Akka file watcher
  val scalatest                     = "org.scalatest"     %% "scalatest"     % "3.1.1" % Test
  val shapeless                     = "com.chuusai"       %% "shapeless"     % "2.3.3" % Test // For shapeless based Reader/Scanner in tests
  val commonsio                     = "commons-io"         % "commons-io"    % "2.6"   % Test // Benchmarks
  val fastjavaio =
    "fastjavaio" % "fastjavaio" % "1.0" % Test from "https://github.com/williamfiset/FastJavaIO/releases/download/v1.0/fastjavaio.jar" //Benchmarks

  val resources = Seq(
    "io.github.er1c"             %% "scala-typesafeequals" % "1.0.0-RC1" % Compile,
    "com.fasterxml.woodstox"      % "woodstox-core"        % "5.1.0",
    "com.github.albfernandez"     % "juniversalchardet"    % "2.3.2",
    "com.typesafe.scala-logging" %% "scala-logging"        % "3.9.2",
    "org.apache.commons"          % "commons-compress"     % "1.9",
    // Used by commons-compress and should be synced up with whatever version commons-compress requires
    "org.tukaani"        % "xz"            % "1.8",
    "org.apache.commons" % "commons-lang3" % "3.4",
    "commons-codec"      % "commons-codec" % "1.10",
    "commons-io"         % "commons-io"    % "2.4",
    "org.xerial.snappy"  % "snappy-java"   % "1.1.1" // SnappyOutputStream might be messed up in 1.1.1.3
  )
}
