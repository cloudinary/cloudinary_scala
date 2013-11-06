package com.cloudinary.parameters

import com.cloudinary.Transformation
import com.cloudinary.EagerTransformation

case class UploadParameters(transformation: Option[Transformation] = None, publicId: Option[String] = None, callback: Option[String] = None,
  format: Option[String] = None, `type`: Option[String] = None, eager: List[Transformation] = List(), customHeaders: Map[String, String] = Map(),
  notificationUrl: Option[String] = None, eagerNotificationUrl: Option[String] = None, proxy: Option[String] = None, folder: Option[String] = None,
  tags: List[String] = List(), backup: Option[Boolean] = None, exif: Option[Boolean] = None, faces: Option[Boolean] = None, colors: Option[Boolean] = None,
  imageMetadata: Option[Boolean] = None, useFilename: Option[Boolean] = None, eagerAsync: Option[Boolean] = None, invalidate: Option[Boolean] = None,
  discardOriginalFilename: Option[Boolean] = None) {
  
  def toMap = Map(
    "transformation" -> transformation.map(_.generate),
    "public_id" -> publicId,
    "callback" -> callback,
    "format" -> format,
    "type" -> `type`,
    "eager" -> (if (eager.size > 0) buildEager(eager) else None),
    "headers" -> (if (customHeaders.size > 0) customHeaders.map(kv => s"${kv._1}: ${kv._2}").mkString("\n") else None),
    "notification_url" -> notificationUrl,
    "eager_notification_url" -> eagerNotificationUrl,
    "proxy" -> proxy,
    "folder" -> folder,
    "tags" -> (if (tags.size > 0) tags.mkString(",") else None),
    "backup" -> backup,
    "exif" -> exif,
    "faces" -> faces,
    "colors" -> colors,
    "image_metadata" -> imageMetadata,
    "use_filename" -> useFilename,
    "eager_async" -> eagerAsync,
    "invalidate" -> invalidate,
    "discard_original_filename" -> discardOriginalFilename)
    
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

case class TextParameters(text:String,
    publicId:Option[String] = None, fontFamily:Option[String] = None, fontSize:Option[Int] = None, fontColor:Option[String] = None, 
    textAlign:Option[String] = None, fontWeight:Option[String] = None, fontStyle:Option[String] = None, background:Option[String] = None, 
    opacity:Option[Int] = None, textDecoration:Option[String] = None) { 
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
      "text_decoration" -> textDecoration
   )
}