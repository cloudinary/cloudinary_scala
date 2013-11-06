package com.cloudinary.response

import java.util.Date
import org.json4s._
import org.json4s.DefaultFormats
import com.cloudinary.Transformation
import java.text.SimpleDateFormat

case class FaceInfo(x: Int, y: Int, width: Int, height: Int)
case class EagerInfo(url: String, secure_url: String)
case class ColorInfo(color:String, rank:Double)
case class SpriteImageInfo(x: Int, y: Int, width: Int, height: Int)
case class UsageInfo(usage: Int, limit: Int, used_percent: Float)
case class DerivedInfo(public_id: String, format: String, bytes: Long, id: String, url: String, secure_url: String)
case class DerivedTransformInfo(transformation: String, format: String, bytes: Long, id: String, url: String, secure_url: String)
case class TransformationInfo(name: String, allowed_for_strict: Boolean, used: Boolean)

//Upload API Responses
case class UploadResponse(public_id: String, url: String, secure_url: String, signature: String, bytes: Int,
  width: Int, height: Int, format: String, resource_type: String) extends AdvancedResponse with VersionedResponse with TimestampedResponse
case class DestroyResponse(result: String) extends RawResponse
case class ExplicitResponse(public_id: String, version: String, url: String, secure_url: String, signature: String, bytes: Long,
  format: String, eager: List[EagerInfo] = List(), `type`: String) extends RawResponse
case class SpriteResponse(css_url: String, secure_css_url: String, image_url: String, json_url: String,
  /*public_id:String,*/ version: String, image_infos: Map[String, SpriteImageInfo]) extends RawResponse
case class MultiResponse(public_id: String, version: String, url: String, secure_url: String) extends RawResponse
case class ExplodeResponse(status: String, batch_id: String) extends RawResponse
case class TextResponse(width: Int, height: Int) extends RawResponse
case class TagResponse(public_ids: List[String]) extends RawResponse

//Admin API Responses
case class PingResponse(status: String)
case class UsageResponse(plan: String, last_updated: Date, objects: UsageInfo, bandwidth: UsageInfo, storage: UsageInfo,
  requests: Int, resources: Int, derived_resources: Int) extends RawResponse
case class ResourceTypesResponse(resource_types: List[String]) extends RawResponse
case class ResourcesResponse(resources: List[ResourceResponse], next_cursor: Option[String]) extends RawResponse
case class DeleteResourceResponse(deleted: Map[String, String]) extends RawResponse
case class TransformationsResponse(transformations: List[TransformationInfo], next_cursor: Option[String]) extends RawResponse
case class TransformationResponse(name: String, allowed_for_strict: Boolean, used: Boolean, derived: List[DerivedInfo]) extends RawResponse {
  def info =
    Transformation(for {
      JArray(l) <- raw \ "info"
    } yield {
      (for {
        JObject(o) <- l
        (key, value) <- o
      } yield {
        key -> value.values
      }).toMap
    })
}
case class TransformationUpdateResponse(message: String)
case class TagsResponse(tags: List[String], next_cursor: Option[String]) extends RawResponse

//Shared
case class ResourceResponse(public_id: String, url: String, secure_url: String, bytes: Int,
  width: Int, height: Int, format: String, resource_type: String, `type`: String,
  derived: List[DerivedTransformInfo] = List(), tags: List[String]) extends AdvancedResponse with VersionedResponse with TimestampedResponse

trait RawResponse {
  private var rawJson: JsonAST.JValue = null
  private[cloudinary] def raw_=(json: JsonAST.JValue) = rawJson = json
  def raw = rawJson
}

trait VersionedResponse extends RawResponse {
  lazy val version = raw \ "version" match {
    case JInt(v) => v.toString
    case JString(v) => v
    case v@_ => v.toString 
  }
}

trait TimestampedResponse extends RawResponse {
  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  lazy val createdAt = {
    (raw \ "created_at") match {
      case JString(s) =>  try {
        dateFormatter.parse(s)
      } catch {
        case _:Throwable => null 
      }
      case _ => null
    }
  }
}

trait AdvancedResponse extends RawResponse {
  implicit val formats = DefaultFormats

  lazy val eager: List[EagerInfo] = (for {
    JArray(l) <- raw \ "eager"
    v <- l
  } yield v.extract[EagerInfo])

  lazy val exif: Map[String, String] = (raw \ "exif") match {
    case JObject(v) => v.collect({ case (k, JString(v)) => k -> v}).toMap
    case _ => Map()
  }

  lazy val metadata: Map[String, String] = (raw \ "image_metadata") match {
    case JObject(v) => v.collect({ case (k, JString(v)) => k -> v}).toMap
    case _ => Map()
  }

  lazy val colors: List[ColorInfo] =
    (raw \ "colors") match {
    	case JArray(l) => l.collect{case JArray(JString(color) :: JDouble(rank) :: Nil) => ColorInfo(color, rank)}
    	case _ => List()
    }

  lazy val predominant:Map[String, List[ColorInfo]] = (raw \ "predominant") match {
    case JObject(v) => v.collect{ case (k, JArray(v)) => k -> v.collect{case JArray(JString(color) :: JDouble(rank) :: Nil) => ColorInfo(color, rank)}}.toMap
    case _ => Map()
  }

  lazy val faces: List[FaceInfo] = (for {
    JArray(l) <- raw \ "faces"
  } yield {
    for {
      JArray(f) <- l
      JInt(c) <- f
    } yield c
  }).toList.collect {
    case x :: y :: width :: height :: Nil => FaceInfo(x.toInt, y.toInt, width.toInt, height.toInt)
  }
}
