package com.cloudinary.parameters

import com.cloudinary.Transformation
import com.cloudinary.EagerTransformation
import com.cloudinary.response.FaceInfo
import com.cloudinary.response.CustomCoordinate
import com.cloudinary.response.ModerationStatus

private [cloudinary] case class FacesInfo(faces:Iterable[FaceInfo])
private [cloudinary] case class CustomCoordinates(coordinates:Iterable[CustomCoordinate])
private [cloudinary] case class ContextMap(context:Map[String, String])
private [cloudinary] case class HeaderMap(headers:Map[String, String])
private [cloudinary] case class Transformations(transformations:List[Transformation])
private [cloudinary] case class StringSet(values:Iterable[String])

trait ParamFactory {
  def toMap:Map[String,String] = parameters.collect{
      case (k, FacesInfo(v)) => k -> v.map(i => s"${i.x},${i.y},${i.width},${i.height}").mkString("|")
      case (k, CustomCoordinates(v)) => k -> v.map(i => s"${i.x},${i.y},${i.width},${i.height}").mkString("|")
      case (k, ContextMap(v)) => k -> v.map(kv => s"${kv._1}=${kv._2}").mkString("|")
      case (k, HeaderMap(v)) => k -> v.map(kv => s"${kv._1}: ${kv._2}").mkString("\n")
      case (k, StringSet(v)) => k -> v.mkString(",")
      case (k, v:Transformation) => k -> v.generate
      case (k, Transformations(v)) => k -> buildEager(v)
      case (k, v) => k -> v.toString
  }.filterNot(p => p._2.isEmpty())

  def apply(key:String) = unboxedValue(parameters(key))
  def get(key:String) = parameters.get(key).map(unboxedValue)

  def apply(key:String, value: Any) = param(key, value)

  def parameters: Map[String, _]
  
  type Self
  protected def factory: Map[String,_] => Self
  protected def param(key:String, value:Any):Self = factory(parameters + (key -> value))
  
  private def buildEager(transformations: Iterable[Transformation]): String =
    transformations.map {
      transformation: Transformation =>
        val transformationString = transformation.generate()
        transformation match {
          case e: EagerTransformation => transformationString + "/" + e.format
          case _ => transformationString
        }
    }.mkString("|")

  private def unboxedValue(value: Any) = value match {
    case FacesInfo(v) => v
    case CustomCoordinates(v) => v
    case ContextMap(v) => v
    case HeaderMap(v) => v
    case StringSet(v) => v
    case Transformations(v) => v
    case v => v
  }
}

trait UpdateableResourceParams extends ParamFactory {
  def headers(value:Map[String, String]) = param("headers" , HeaderMap(value))
  def tags(value:Set[String]) = param("tags" , StringSet(value))
  def faceCoordinates(value:List[FaceInfo]) = param("face_coordinates" , FacesInfo(value))
  def customCoordinates(value:List[CustomCoordinate]) = param("custom_coordinates" , CustomCoordinates(value))
  def context(value:Map[String, String]) = param("context" , ContextMap(value))
  def rawConvert(source:String) = param("raw_convert", source)
  def categorization(source:String) = param("categorization", source)
  def detection(source:String) = param("detection", source)
  def backgroundRemoval(source:String) = param("background_removal", source)
  def autoTagging(threshold:Double) = param("auto_tagging", threshold)
}

trait UploadableResourceParams extends ParamFactory {
  def transformation(value:Transformation) = param("transformation", value)
  def publicId(value:String) = param("public_id" , value)
  def callback(value:String) = param("callback" , value)
  def format(value:String) = param("format" , value)
  def `type`(value:String) = param("type" , value)
  def eager(value:List[Transformation]) = param("eager" , Transformations(value))  
  def notificationUrl(value:String) = param("notification_url" , value)
  def eagerNotificationUrl(value:String) = param("eager_notification_url" , value)
  def proxy(value:String) = param("proxy" , value)
  def folder(value:String) = param("folder" , value)
  def backup(backup:Boolean) = param("backup" , backup)
  def exif(value:Boolean) = param("exif" , value)
  def faces(value:Boolean) = param("faces" , value)
  def colors(value:Boolean) = param("colors" , value)
  def useFilename(value:Boolean) = param("use_filename" , value)
  def uniqueFilename(value:Boolean) = param("unique_filename" , value)
  def invalidate(value:Boolean) = param("invalidate" , value)
  def discardOriginalFilename(value:Boolean) = param("discard_original_filename" , value)
  def overwrite(value:Boolean) = param("overwrite" , value)
  def imageMetadata(value:Boolean) = param("image_metadata" , value)
  def eagerAsync(value:Boolean) = param("eager_async" , value)
  def allowedFormats(value:Set[String]) = param("allowed_formats" , StringSet(value))
  def moderation(kind:String) = param("moderation", kind)
  def pHash(value:Boolean) = param("phash", value)
  def uploadPreset(value:String) = param("upload_preset", value)
  def disallowPublicId(value:Boolean) = param("disallow_public_id", value)
  def returnDeleteToken(value:Boolean) = param("return_delete_token", value)
}

case class UpdateParameters(parameters: Map[String, _] = Map()) extends UpdateableResourceParams{
  type Self = UpdateParameters
  protected val factory = UpdateParameters.apply _
  def moderationStatus(status:ModerationStatus.Value) = param("moderation_status", status.toString)
}

case class UploadParameters(parameters: Map[String, _] = Map(), signed:Boolean = true) extends UploadableResourceParams with UpdateableResourceParams {
  type Self = UploadParameters
  protected val factory = (p: Map[String, _]) => UploadParameters(p, signed)
}

case class LargeUploadParameters(parameters: Map[String, _] = Map()) extends ParamFactory {
  type Self = LargeUploadParameters
  protected val factory = LargeUploadParameters.apply _
  def `type`(value:String) = param("type" , value)
  def publicId(value:String) = param("public_id" , value)
  def backup(backup:Boolean) = param("backup" , backup)
  def uploadId(value:String) = param("upload_id" , value)
}

case class TextParameters(text: String,
  publicId: Option[String] = None, fontFamily: Option[String] = None, fontSize: Option[Int] = None, fontColor: Option[String] = None,
  textAlign: Option[String] = None, fontWeight: Option[String] = None, fontStyle: Option[String] = None, background: Option[String] = None,
  opacity: Option[Int] = None, textDecoration: Option[String] = None) {
  def toMap = Map(
    "text" -> text,
    "public_id" -> publicId,
    "font_family" -> fontFamily,
    "font_size" -> fontSize,
    "font_color" -> fontColor,
    "text_align" -> textAlign,
    "font_weight" -> fontWeight,
    "font_style" -> fontStyle,
    "background" -> background,
    "opacity" -> opacity,
    "text_decoration" -> textDecoration)
}