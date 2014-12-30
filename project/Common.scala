object Common {
  def version = "0.9.6-SNAPSHOT"  
  def playVersion = System.getProperty("play.version", "2.3.7")
  def scalaVersion =  "2.11.4"
  def scalaVersions =  Seq("2.10.4", scalaVersion)
}