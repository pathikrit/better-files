resolvers += "jgit-repo" at "http://download.eclipse.org/jgit/maven"
//TODO: replace these with https://github.com/paypal/horizon
addSbtPlugin("com.eed3si9n"       %   "sbt-unidoc"      % "0.3.2")
addSbtPlugin("com.timushev.sbt"   %   "sbt-updates"     % "0.1.9")
addSbtPlugin("com.typesafe.sbt"   %   "sbt-ghpages"     % "0.5.4")
addSbtPlugin("me.lessis"          %   "bintray-sbt"     % "0.3.0")
addSbtPlugin("org.scoverage"      %   "sbt-scoverage"   % "1.1.0")
///addSbtPlugin("com.codacy"         %   "sbt-codacy-coverage" % "1.2.0")
