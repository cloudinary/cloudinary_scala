// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % System.getProperty("play.version", "2.6.7"))

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

// prefer to depend on these in ~/.sbt since this is only relevant for releasers

// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")

// addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
