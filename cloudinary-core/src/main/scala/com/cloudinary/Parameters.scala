package com.cloudinary.parameters

import com.cloudinary.Transformation
import com.cloudinary.EagerTransformation
import com.cloudinary.response.FaceInfo
import com.cloudinary.response.ModerationStatus

trait ParamFactory[T <: ParamFactory[T] ] { self:T =>
  def toMap:Map[String,String] = parameters.filterNot(p => p._2.isEmpty())
  def parameters: Map[String, String]
  protected def param(key:String, value:String): T
}

trait UpdateableResourceParams[T <: UpdateableResourceParams[T]] extends ParamFactory[T] { self:T =>

  def headers(value:Map[String, String]) = param("headers" , value.map(kv => s"${kv._1}: ${kv._2}").mkString("\n"))
  def tags(value:Set[String]) = param("tags" , value.mkString(","))
  def faceCoordinates(value:List[FaceInfo]) = param("face_coordinates" , value.map(i => s"${i.x},${i.y},${i.width},${i.height}").mkString("|"))
  def context(value:Map[String, String]) = param("context" , value.map(kv => s"${kv._1}=${kv._2}").mkString("|"))
  def ocr(source:String) = param("ocr", source)
  def rawConvert(source:String) = param("raw_convert", source)
  def categorization(source:String) = param("categorization", source)
  def detection(source:String) = param("detection", source)
  def similaritySearch(source:String) = param("similarity_search", source)
  def autoTagging(threshold:Double) = param("auto_tagging", threshold.toString)
}

case class UpdateParameters(parameters: Map[String, String] = Map()) extends UpdateableResourceParams[UpdateParameters]{
  protected def param(key:String, value:String) = UpdateParameters(parameters + (key -> value))
  def moderationStatus(status:ModerationStatus.Value) = param("moderation_status", status.toString)
}

case class UploadParameters(parameters: Map[String, String] = Map()) extends UpdateableResourceParams[UploadParameters] {
  protected def param(key:String, value:String) = UploadParameters(parameters + (key -> value))
  def transformation(value:Transformation) = param("transformation", value.generate)
  def publicId(value:String) = param("public_id" , value)
  def callback(value:String) = param("callback" , value)
  def format(value:String) = param("format" , value)
  def `type`(value:String) = param("type" , value)
  def eager(value:List[Transformation]) = param("eager" , buildEager(value))  
  def notificationUrl(value:String) = param("notification_url" , value)
  def eagerNotificationUrl(value:String) = param("eager_notification_url" , value)
  def proxy(value:String) = param("proxy" , value)
  def folder(value:String) = param("folder" , value)
  def backup(backup:Boolean) = param("backup" , backup.toString)
  def exif(value:Boolean) = param("exif" , value.toString)
  def faces(value:Boolean) = param("faces" , value.toString)
  def colors(value:Boolean) = param("colors" , value.toString)
  def useFilename(value:Boolean) = param("use_filename" , value.toString)
  def uniqueFilename(value:Boolean) = param("unique_filename" , value.toString)
  def invalidate(value:Boolean) = param("invalidate" , value.toString)
  def discardOriginalFilename(value:Boolean) = param("discard_original_filename" , value.toString)
  def overwrite(value:Boolean) = param("overwrite" , value.toString)
  def imageMetadata(value:Boolean) = param("image_metadata" , value.toString)
  def eagerAsync(value:Boolean) = param("eager_async" , value.toString)
  def allowedFormats(value:Set[String]) = param("allowed_formats" , value.mkString(","))
  def moderation(kind:String) = param("moderation", kind)
  

  private def buildEager(transformations: Iterable[Transformation]): String =
    transformations.map {
      transformation: Transformation =>
        val transformationString = transformation.generate()
        transformation match {
          case e: EagerTransformation => transformationString + "/" + e.format
          case _ => transformationString
        }
    }.mkString("|")
}

case class LargeUploadParameters(parameters: Map[String, String] = Map()) extends ParamFactory[LargeUploadParameters] {
  protected def param(key:String, value:String) = LargeUploadParameters(parameters + (key -> value))
  def `type`(value:String) = param("type" , value)
  def publicId(value:String) = param("public_id" , value)
  def backup(backup:Boolean) = param("backup" , backup.toString)
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