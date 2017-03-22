package cloudinary.plugin

import javax.inject._
import play.api.{ Configuration, Environment }
import com.google.inject.AbstractModule
import com.cloudinary.Cloudinary

@Singleton
class CloudinaryPlugin @Inject() (
  configuration: Configuration) extends AbstractModule {
  def configure() = {}
  lazy val cloudinary = configuration.getOptional[String]("cloudinary.url") match {
    case Some(url) => 
      new Cloudinary(url)
    case None =>
      new Cloudinary(Map(
          "api_key" -> configuration.getOptional[String]("cloudinary.api_key").getOrElse(throw new IllegalArgumentException("Configuration must include api_key")),
          "api_secret" -> configuration.getOptional[String]("cloudinary.api_secret").getOrElse(throw new IllegalArgumentException("Configuration must include api_secret")),
          "cloud_name" -> configuration.getOptional[String]("cloudinary.cloud_name").getOrElse(throw new IllegalArgumentException("Configuration must include cloud_name")),
          "cname" -> configuration.getOptional[String]("cloudinary.cname"),
          "secure_distribution" -> configuration.getOptional[String]("cloudinary.secure_distribution"),
          "private_cdn" -> configuration.getOptional[Boolean]("cloudinary.private_cdn").getOrElse(false),
          "secure" -> configuration.getOptional[Boolean]("cloudinary.secure").getOrElse(false),
          "cdn_subdomain" -> configuration.getOptional[Boolean]("cloudinary.cdn_subdomain").getOrElse(false),
          "shorten" -> configuration.getOptional[Boolean]("cloudinary.shorten").getOrElse(false)
      ))
  }
}