package cloudinary.model

import play.api.Play.current
import cloudinary.plugin.CloudinaryPlugin
import com.cloudinary.Cloudinary
import com.cloudinary.Transformation
import java.io.File
import com.cloudinary.parameters.UploadParameters
import scala.concurrent.ExecutionContext
import com.cloudinary.response.UploadResponse
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import play.api.data.Mapping
import play.api.data.format.Formatter
import play.api.data.FieldMapping
import play.api.data.FormError

case class CloudinaryResource(version: String, publicId: String, format:Option[String] = None, data:Option[UploadResponse] = None,
    resourceType:Option[String] = None, `type`:Option[String] = None, signature:Option[String] = None) {
  
  def identifier = 
		  s"v$version/$publicId" +
		  format.map("." + _).getOrElse("")
		  
  def preloadedIdentifier = 
		  resourceType.map(_ + "/").getOrElse("") +
		  `type`.map(_ + "/").getOrElse("") +
		  identifier +
		  signature.map("#" + _).getOrElse("")
		  
  def url(transformation:Option[Transformation] = None, formatOverride:Option[String] = None) = cp.cloudinary.url.copy(format=formatOverride.orElse(format), transformation=transformation).version(version).generate(publicId)
  private def cp = current.plugin[CloudinaryPlugin].getOrElse(throw CloudinaryResource.pluginNotConfiguredException)
}

object CloudinaryResource {
  val preloadedPattern = """^([^\/]+)\/([^\/]+)\/v(\d+)\/([^#]+)#([^\/]+)$""".r
  val storedPattern = """^v(\d+)\/([^#]+)$""".r

  def pluginNotConfiguredException = new Exception("Cloudinary plugin isn't initialized. Please make sure to enable it")

  def upload(file: AnyRef, params: UploadParameters)(implicit executionContext: ExecutionContext) = {
    val cp = current.plugin[CloudinaryPlugin].getOrElse(throw pluginNotConfiguredException)
    val cloudinary = cp.cloudinary
    cloudinary.uploader.upload(file, params).map {
      response =>
        CloudinaryResource(response.version, response.public_id, Some(response.format).filterNot(_ == ""), Some(response))
    }
  }

  def preloaded(uri: String):Either[String, CloudinaryResource] = uri match {
      case preloadedPattern(resourceType, rtype, version, filename, signature) if verifySignature(version, filename, signature) => {
        val (publicId, format) = splitFormat(filename)
        val maybeFormat = Some(format).filterNot(_ == "")
        Right(CloudinaryResource(version, publicId, maybeFormat, 
            resourceType = Some(resourceType), `type` = Some(rtype), signature = Some(signature)))
      }
      case _ => Left("Signature of preloaded files does not match expected")
    }
  
  def stored(uri:String) = uri match {
    case storedPattern(version, filename) => 
      val (publicId, format) = splitFormat(filename)
      val maybeFormat = Some(format).filterNot(_ == "")
      CloudinaryResource(version, publicId, maybeFormat)
    case _ => throw new IllegalArgumentException("URI of preloaded file is illegal")
  }
  
  val preloadedMapping:Mapping[CloudinaryResource] = FieldMapping[CloudinaryResource]()(preloadedFormatter) 
  
  implicit def preloadedFormatter = new Formatter[CloudinaryResource] {
     override val format = None
     def bind(key: String, data: Map[String, String]) = data.get(key) match {
       case Some(value:String) => preloaded(value).left.map(e => Seq(FormError(key, e, Nil)))
       case None => Right(null)
     }
     def unbind(key: String, value: CloudinaryResource) = if (value != null) {
       Map(key -> value.preloadedIdentifier)
     } else {
       Map()
     }
  }
  
  val validPreloadedSignature = Constraint[String] {s:String =>
  	preloaded(s) match {
  	  case Left(e) => Invalid(e)
  	  case _ => Valid
  	}
  }

  private def splitFormat(identifier: String) = {
    val lastDot = identifier.lastIndexOf('.')
    if (lastDot > -1) {
      (identifier.substring(0, lastDot), identifier.substring(lastDot+1))
    } else {
      (identifier, "")
    }
  }

  private def verifySignature(version: String, filename: String, signature: String) = {
    val cloudinary = current.plugin[CloudinaryPlugin].map(_.cloudinary)
    val (publicId, format) = splitFormat(filename)
    val params = Map("public_id" -> publicId, "version" -> version)
    cloudinary match {
      case Some(c: Cloudinary) =>
        c.signRequest(params)("signature") == signature
      case _ => throw pluginNotConfiguredException
    }
  }
}