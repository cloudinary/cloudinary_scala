version := Common.version

scalaVersion := Common.scalaVersion

crossScalaVersions := Common.scalaVersions

lazy val cloudinaryCoreScala =  project.in( file("cloudinary-core") )

lazy val cloudinaryPlayPlugin =  project.in( file("cloudinary-play-plugin") ).enablePlugins(PlayScala).dependsOn(cloudinaryCoreScala)

lazy val photoAlbumScala =  project.in( file("samples/photo_album") ).settings(publishArtifact := false).enablePlugins(PlayScala).dependsOn(cloudinaryPlayPlugin)

lazy val root = project.in( file(".") ).aggregate(cloudinaryCoreScala, cloudinaryPlayPlugin).settings(
     aggregate in update := false
   ).settings(publishArtifact := false)