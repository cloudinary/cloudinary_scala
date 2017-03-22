name := "photo_album_scala"

version := Common.version

scalaVersion := Common.scalaVersion

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.188",
  "com.typesafe.play" %% "play-slick" % "3.0.0-M3",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0-M3",
  "org.webjars" %% "webjars-play" % "2.6.0-M1",
  "org.webjars.npm" % "jquery" % "2.2.4",
  "org.webjars.npm" % "blueimp-file-upload" % "9.12.1",
  "org.webjars.npm" % "cloudinary-jquery-file-upload" % "2.2.1",
  evolutions
)     

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

routesGenerator := InjectedRoutesGenerator