object Common {
  def version = "1.2.1"
  def playVersion = System.getProperty("play.version", "2.6.7")
  def scalaVersion =  "2.12.4"
  def scalaVersions =  Seq("2.11.11", scalaVersion)
}
