name := "cloudinary-play-module"

organization := "com.cloudinary"

version := "0.9-SNAPSHOT"

resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)

libraryDependencies ++= Seq("com.cloudinary" %% "cloudinary-core-scala" % "0.9-SNAPSHOT")    

play.Project.playScalaSettings
