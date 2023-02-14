addSbtPlugin("com.github.sbt"            % "sbt-ci-release" % "1.5.11")
addSbtPlugin("org.scoverage"             % "sbt-scoverage"  % "2.0.7")
addSbtPlugin("com.timushev.sbt"          % "sbt-updates"    % "0.4.2")
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat"   % "0.4.2")
addSbtPlugin("org.scalameta"             % "sbt-scalafmt"   % "2.5.0")

// TODO: A workaround until sbt ecosystem migrate to scala-xml 2.x https://github.com/sbt/sbt/issues/6997
ThisBuild / libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
