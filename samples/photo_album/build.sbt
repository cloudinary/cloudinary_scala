name := "photo_album_scala"

version := Common.version

scalaVersion := Common.scalaVersion

val playSlickVersion = "3.0.2"

libraryDependencies ++= Seq(
  "com.h2database" % "h2" % "1.4.188",
  "com.typesafe.play" %% "play-slick" % playSlickVersion,
  "com.typesafe.play" %% "play-slick-evolutions" % playSlickVersion,
  evolutions
)

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := "_*.less"

routesGenerator := InjectedRoutesGenerator