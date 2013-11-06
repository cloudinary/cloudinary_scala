name := "photo_album_scala"

version := "0.9-SNAPSHOT"

libraryDependencies ++= Seq(
  "com.cloudinary" %% "cloudinary-play-module" % "0.9-SNAPSHOT",
  "com.typesafe.play" %% "play-slick" % "0.5.0.8"
)     

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

play.Project.playScalaSettings
