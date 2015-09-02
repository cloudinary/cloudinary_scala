package cloudinary.plugin

import javax.inject._
import play.api.{ Configuration, Environment }
import com.google.inject.AbstractModule
import com.cloudinary.Cloudinary

@Singleton
class CloudinaryPlugin @Inject() (
  configuration: Configuration) extends AbstractModule {
  def configure() = {}
  lazy val cloudinary = configuration.getString("cloudinary.url") match {
    case Some(url) => 
      new Cloudinary(url)
    case None =>
      new Cloudinary(Map(
          "api_key" -> configuration.getString("cloudinary.api_key").getOrElse(throw new IllegalArgumentException("Configuration must include api_key")),
          "api_secret" -> configuration.getString("cloudinary.api_secret").getOrElse(throw new IllegalArgumentException("Configuration must include api_secret")),
          "cloud_name" -> configuration.getString("cloudinary.cloud_name").getOrElse(throw new IllegalArgumentException("Configuration must include cloud_name")),
          "cname" -> configuration.getString("cloudinary.cname"),
          "secure_distribution" -> configuration.getString("cloudinary.secure_distribution"),
          "private_cdn" -> configuration.getBoolean("cloudinary.private_cdn").getOrElse(false),
          "secure" -> configuration.getBoolean("cloudinary.secure").getOrElse(false),
          "cdn_subdomain" -> configuration.getBoolean("cloudinary.cdn_subdomain").getOrElse(false),
          "shorten" -> configuration.getBoolean("cloudinary.shorten").getOrElse(false)
      ))
  }
}