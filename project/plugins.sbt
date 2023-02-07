addSbtPlugin("com.github.sbt"            % "sbt-ci-release" % "1.5.11")
addSbtPlugin("com.github.sbt"            % "sbt-unidoc"     % "0.5.0")
addSbtPlugin("org.scoverage"             % "sbt-scoverage"  % "2.0.6")
addSbtPlugin("com.timushev.sbt"          % "sbt-updates"    % "0.4.2")
addSbtPlugin("com.github.sbt"            % "sbt-ghpages"    % "0.7.0")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"   % "0.1.22")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"   % "2.5.0")

// TODO: A workaround until sbt ecosystem migrate to scala-xml 2.x https://github.com/sbt/sbt/issues/6997
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)
