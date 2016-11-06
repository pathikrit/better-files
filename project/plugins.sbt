resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.eed3si9n"       %   "sbt-unidoc"              % "0.3.2")
addSbtPlugin("com.github.gseitz"  %   "sbt-release"             % "1.0.0")
addSbtPlugin("com.jsuereth"       %   "sbt-pgp"                 % "1.0.0")
//addSbtPlugin("org.scoverage"      %   "sbt-scoverage"           % "1.1.0")
addSbtPlugin("com.timushev.sbt"   %   "sbt-updates"             % "0.1.9")
addSbtPlugin("com.typesafe.sbt"   %   "sbt-ghpages"             % "0.5.4")
addSbtPlugin("com.updateimpact"   %   "updateimpact-sbt-plugin" % "2.1.1")
addSbtPlugin("org.xerial.sbt"     %   "sbt-sonatype"            % "0.5.1")
//addSbtPlugin("com.codacy"         %   "sbt-codacy-coverage" % "1.2.0")
//TODO: Run sbt dependencyUpdates when we drop Scala 2.10
