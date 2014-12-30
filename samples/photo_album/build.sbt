name := "photo_album_scala"

version := Common.version

libraryDependencies ++= Seq(
  "com.cloudinary" %% "cloudinary-scala-play" % version.value,
  "com.typesafe.play" %% "play-slick" % "0.8.1"
)     

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

resolvers += "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
