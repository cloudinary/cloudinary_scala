object Common {
  def version = "1.2.2"
  def playVersion = System.getProperty("play.version", "2.4.2")
  def scalaVersion =  "2.11.5"
  def scalaVersions =  Seq("2.10.4", scalaVersion)
}
