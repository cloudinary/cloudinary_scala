package cloudinary.plugin

import scala.sys.process._
import play.api.Plugin
import play.api.Application
import play.api.Logger
import com.cloudinary.Cloudinary
import com.cloudinary.Transformation
import play.api.Mode
import java.io.File


class CloudinaryPlugin(app: play.api.Application) extends Plugin { 
  lazy val cloudinary = new Cloudinary(Map(
      "api_key" -> app.configuration.getString("cloudinary.api_key").getOrElse(throw new IllegalArgumentException("Configuration must include api_key")),
      "api_secret" -> app.configuration.getString("cloudinary.api_secret").getOrElse(throw new IllegalArgumentException("Configuration must include api_secret")),
      "cloud_name" -> app.configuration.getString("cloudinary.cloud_name").getOrElse(throw new IllegalArgumentException("Configuration must include cloud_name")),
      "cname" -> app.configuration.getString("cloudinary.cname"),
      "secure_distribution" -> app.configuration.getString("cloudinary.secure_distribution"),
      "private_cdn" -> app.configuration.getBoolean("cloudinary.private_cdn").getOrElse(false),
      "secure" -> app.configuration.getBoolean("cloudinary.secure").getOrElse(false),
      "cdn_subdomain" -> app.configuration.getBoolean("cloudinary.cdn_subdomain").getOrElse(false),
      "shorten" -> app.configuration.getBoolean("cloudinary.shorten").getOrElse(false)
  ))
  override lazy val enabled = {
    !app.configuration.getString("cloudinaryplugin").filter(_ == "disabled").isDefined
  }
  
  override def onStart() {
    if (app.mode == Mode.Dev) {
      //fetch required assets
      if (app.getExistingFile("app/assets/javascripts/cloudinary").isEmpty) {
        val jsDir = app.getFile("app/assets/javascripts")
        app.getExistingFile("app/assets/javascripts/cloudinary").map(_.delete())
        jsDir.mkdirs()
        val workingDir = jsDir.getAbsolutePath()
        Logger.info(s"Downloading cloudinary assets to $workingDir")
        (Seq("curl", "-L", "https://github.com/cloudinary/cloudinary_js/tarball/master") #| s"tar zxvf - -C $workingDir --strip=1").!
        Logger.info(s"Moving files into place:")
        Seq("mv", workingDir + "/js", workingDir + "/cloudinary").!
        Seq("mv", workingDir + "/html/cloudinary_cors.html", workingDir + "/../../assets/cloudinary_cors.html").!
        Seq("rm", "-R", workingDir + "/test").!
        Seq("rm", "-R", workingDir + "/html").!
      }
    }
    cloudinary
  }
}
