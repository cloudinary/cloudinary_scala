import sbt.{ AutoPlugin, Def }
import sbt.Keys._

object Common
  extends AutoPlugin {

  override def trigger = allRequirements

  object autoImport {
    def playVersion = System.getProperty("play.version", "2.6.12")
  }

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      organization := "com.cloudinary",
      version := "1.3.0-SNAPSHOT",
      scalaVersion := "2.12.5",
      crossScalaVersions := Seq(scalaVersion.value, "2.11.12")
    )
}
