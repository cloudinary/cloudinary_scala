// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % System.getProperty("play.version", "2.8.8"))

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

// prefer to depend on these in ~/.sbt since this is only relevant for releasers

// addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.1")

// addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
