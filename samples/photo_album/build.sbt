name := "photo_album_scala"

version := "0.9.2-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.cloudinary" %% "cloudinary-scala-play" % "0.9.2-SNAPSHOT",
  "com.typesafe.play" %% "play-slick" % "0.6.0.1"
)     

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

play.Project.playScalaSettings
