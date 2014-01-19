import sbt._
import Keys._
  
object BuildSettings {
  val buildOrganization = "com.cloudinary"
  val buildVersion = "0.9.1-SNAPSHOT"
  val buildScalaVersion = "2.10.2"
    
  val buildSettings = Defaults.defaultSettings ++ Seq (
    organization := buildOrganization,
    version      := buildVersion,
    scalaVersion := buildScalaVersion
  )
}

object Resolvers {
  val sonatypeSnapshots = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val sonatypeReleases = "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
}

object Dependencies {
  val asyncHttp = "com.ning" % "async-http-client" % "1.7.19"
  val json4s = "org.json4s" %% "json4s-native" % "3.2.5"
  val scalaTest = "org.scalatest" % "scalatest_2.10" % "1.9.2" % "test"
}


object CloudinaryBuild extends Build {
  import Resolvers._
  import Dependencies._
  import BuildSettings._
  
  lazy val root = Project (
    "cloudinary-core-scala",
    file ("."),
    settings = buildSettings ++ Seq(
        libraryDependencies ++= Seq(asyncHttp, json4s, scalaTest),
        resolvers ++= Seq(sonatypeSnapshots, sonatypeReleases),
        scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")
    )
  )
}
