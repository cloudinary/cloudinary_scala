object Common {
  def version = "0.9.7-SNAPSHOT"
  def playVersion = System.getProperty("play.version", "2.8.8")
  def scalaVersion =  "2.13.3"
  def scalaVersions =  Seq("2.10.4", scalaVersion)
}
