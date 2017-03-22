import sbt._
import Keys._

organization := "com.cloudinary"

version := Common.version

scalaVersion := Common.scalaVersion

crossScalaVersions := Common.scalaVersions

name := "cloudinary-core-scala"

pomExtra := {
  <url>http://cloudinary.com</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:github.com/cloudinary/cloudinary_scala.git</connection>
    <developerConnection>scm:git:github.com/cloudinary/cloudinary_scala.git</developerConnection>
    <url>github.com/cloudinary/cloudinary_scala.git</url>
  </scm>
  <developers>
     <developer>
        <id>cloudinary</id>
        <name>Cloudinary</name>
        <email>info@cloudinary.com</email>
    </developer>
  </developers>
}  
  
libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.9.40",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.json4s" %% "json4s-ext" % "3.5.0",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0",
  "org.nanohttpd" % "nanohttpd" % "2.2.0" % "test")

// http://mvnrepository.com/artifact/org.slf4j/slf4j-simple
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.21" % "test"
libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test"
resolvers ++= Seq("sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots", "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

