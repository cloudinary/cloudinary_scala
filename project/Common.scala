object Common {
  def version = "1.2.2"
  def playVersion = System.getProperty("play.version", "2.6.6")
  def scalaVersion =  "2.12.8"
  def scalaVersions =  Seq("2.10.4", "2.11.5", scalaVersion)
}
