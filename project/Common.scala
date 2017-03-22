object Common {
  def version = "1.1.2-SNAPSHOT"
  def playVersion = System.getProperty("play.version", "2.6.0-M2")
  def scalaVersion =  "2.12.1"
  def scalaVersions =  Seq("2.11.8", scalaVersion)
}