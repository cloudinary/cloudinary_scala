import sbt._
import Keys._
import SonatypeKeys._

sonatypeSettings

organization := "com.cloudinary"

version := "0.9.1-SNAPSHOT"

scalaVersion := "2.10.2"

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
  
libraryDependencies ++= Seq("com.ning" % "async-http-client" % "1.7.19", "org.json4s" %% "json4s-native" % "3.2.5", "org.scalatest" % "scalatest_2.10" % "1.9.2" % "test")

resolvers ++= Seq("sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots", "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases")

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
