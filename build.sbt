lazy val core =
  project
    .settings(
      name := "scala-core",
      libraryDependencies ++= Seq(
        "com.ning" % "async-http-client" % "1.9.40",
        "org.json4s" %% "json4s-native" % "3.5.3",
        "org.json4s" %% "json4s-ext" % "3.5.3",
        "org.scalatest" %% "scalatest" % "3.0.4" % "test",
        "org.nanohttpd" % "nanohttpd" % "2.2.0" % "test"
      ),

      // http://mvnrepository.com/artifact/org.slf4j/slf4j-simple
      libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21" % "test",
      libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % "test",
      scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
    )

lazy val playPlugin =
  project
    .in(file("play-plugin"))
    .enablePlugins(PlayScala)
    .dependsOn(core)
    .settings(
      name := "scala-play-plugin",
      libraryDependencies += "com.google.inject" % "guice" % "4.1.0"
    )

lazy val sample =
  project
    .in(file("samples/photo_album"))
    .settings(
      publishArtifact := false,
      libraryDependencies ++= Seq(
        "com.h2database" % "h2" % "1.4.188",
        "com.typesafe.play" %% "play-slick" % "3.0.3",
        "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
        evolutions
      ),
      includeFilter in (Assets, LessKeys.less) := "*.less",
      excludeFilter in (Assets, LessKeys.less) := "_*.less",
      routesGenerator := InjectedRoutesGenerator
    )
    .enablePlugins(PlayScala)
    .dependsOn(playPlugin)

lazy val root =
  project
    .in(file("."))
    .aggregate(
      core,
      playPlugin,
      sample
    )
    .settings(
      aggregate in update := false,
      publishArtifact := false
   )
