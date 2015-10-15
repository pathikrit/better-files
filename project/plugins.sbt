resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"

addSbtPlugin("com.eed3si9n"       %   "sbt-unidoc"      % "0.3.2")
addSbtPlugin("com.jsuereth"       %   "sbt-pgp"         % "1.0.0")
addSbtPlugin("com.github.gseitz"  %   "sbt-release"     % "1.0.0")
addSbtPlugin("com.timushev.sbt"   %   "sbt-updates"     % "0.1.9")
addSbtPlugin("com.typesafe.sbt"   %   "sbt-ghpages"     % "0.5.4")
addSbtPlugin("org.scoverage"      %   "sbt-scoverage"   % "1.1.0")
///addSbtPlugin("com.codacy"         %   "sbt-codacy-coverage" % "1.2.0")
