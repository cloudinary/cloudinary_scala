package com.cloudinary.parameters

import com.cloudinary.Transformation
import com.cloudinary.EagerTransformation
import com.cloudinary.response.FaceInfo

case class StoreParameters(publicId: Option[String] = None, `type`: Option[String] = None, folder: Option[String] = None, 
    discardOriginalFilename: Option[Boolean] = None, useFilename: Option[Boolean] = None, 
    uniqueFilename: Option[Boolean] = None, overwrite: Option[Boolean] = None)
    
case class EagerParameters(eager: List[Transformation] = List(), eagerNotificationUrl: Option[String] = None, 
    eagerAsync: Option[Boolean] = None)
    
case class AnnotationParameters(customHeaders: Map[String, String] = Map(), tags: List[String] = List(), 
    faceCoordinates: List[FaceInfo] = List(), context: Map[String, String] = Map())
    
case class InformationParameters(exif: Option[Boolean] = None, faces: Option[Boolean] = None, 
    colors: Option[Boolean] = None, imageMetadata: Option[Boolean] = None)
    
case class FormatParameters(format: Option[String] = None, allowedFormats: Set[String] = Set())

case class LifecycleParameters(callback: Option[String] = None, notificationUrl: Option[String] = None, backup: Option[Boolean] = None, invalidate: Option[Boolean] = None)

case class UploadParameters(parameters: Map[String, String] = Map()) {
  def toMap:Map[String,String] = parameters.filterNot(p => p._2.isEmpty())
  protected def param(key:String, value:String) = UploadParameters(parameters + (key -> value))
  def transformation(value:Transformation) = param("transformation", value.generate)
  def publicId(value:String) = param("public_id" , value)
  def callback(value:String) = param("callback" , value)
  def format(value:String) = param("format" , value)
  def `type`(value:String) = param("type" , value)
  def eager(value:List[Transformation]) = param("eager" , buildEager(value))
  def headers(value:Map[String, String]) = param("headers" , value.map(kv => s"${kv._1}: ${kv._2}").mkString("\n"))
  def notificationUrl(value:String) = param("notification_url" , value)
  def eagerNotificationUrl(value:String) = param("eager_notification_url" , value)
  def proxy(value:String) = param("proxy" , value)
  def folder(value:String) = param("folder" , value)
  def tags(value:Set[String]) = param("tags" , value.mkString(","))
  def backup(backup:Boolean) = param("backup" , backup.toString)
  def exif(value:Boolean) = param("exif" , value.toString)
  def faces(value:Boolean) = param("faces" , value.toString)
  def colors(value:Boolean) = param("colors" , value.toString)
  def useFilename(value:Boolean) = param("use_filename" , value.toString)
  def unique_filename(value:Boolean) = param("unique_filename" , value.toString)
  def invalidate(value:Boolean) = param("invalidate" , value.toString)
  def discardOriginalFilename(value:Boolean) = param("discard_original_filename" , value.toString)
  def overwrite(value:Boolean) = param("overwrite" , value.toString)
  def imageMetadata(value:Boolean) = param("image_metadata" , value.toString)
  def eagerAsync(value:Boolean) = param("eager_async" , value.toString)
  def faceCoordinates(value:List[FaceInfo]) = param("face_coordinates" , value.map(i => s"${i.x},${i.y},${i.width},${i.height}").mkString("|"))
  def context(value:Map[String, String]) = param("context" , value.map(kv => s"${kv._1}=${kv._2}").mkString("|"))
  def allowedFormats(value:Set[String]) = param("allowed_formats" , value.mkString(","))

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