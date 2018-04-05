package com.cloudinary.response

import java.text.SimpleDateFormat
import java.util.Date

import com.cloudinary.Transformation
import com.cloudinary.parameters._
import org.json4s.{DefaultFormats, _}
import org.json4s.ext.EnumNameSerializer

case class FaceInfo(x: Int, y: Int, width: Int, height: Int)
case class CustomCoordinate(x: Int, y: Int, width: Int, height: Int)
case class EagerInfo(url: String, secure_url: String)
case class ColorInfo(color: String, rank: Double)
case class SpriteImageInfo(x: Int, y: Int, width: Int, height: Int)
case class UsageInfo(usage: Int, limit: Int, used_percent: Float)
case class DerivedInfo(public_id: String, format: String, bytes: Long, id: String, url: String, secure_url: String)
case class DerivedTransformInfo(transformation: String, format: String, bytes: Long, id: String, url: String, secure_url: String)
case class TransformationInfo(name: String, allowed_for_strict: Boolean, used: Boolean)
case class UploadPreset(name:String = null, unsigned:Boolean = false, settings:UploadParameters) {
  def toMap = settings.toMap + ("name" -> name) + ("unsigned" -> unsigned.toString())
}
case class UnparsedUploadPreset(name:String, unsigned:Boolean, settings:Map[String,String])

object ModerationStatus extends Enumeration {
  type ModerationStatus = Value
  val pending, rejected, approved, overridden = Value
}
case class ModerationItem(status: ModerationStatus.Value, kind: String, response: Option[String], updated_at: Option[Date])

object Status extends Enumeration {
  type Status = Value
  val active, pending, deleted = Value
}

object AccessMode extends Enumeration {
  type AccessMode = Value
  val public, authenticated = Value
}

//Upload API Responses
case class UploadResponse(public_id: String, url: String, secure_url: String, signature: String, bytes: Long,
  resource_type: String) extends AdvancedResponse with VersionedResponse with TimestampedResponse {
  override implicit val formats = DefaultFormats + new EnumNameSerializer(ModerationStatus)
  def width:Int = (raw \ "width").extractOpt[Int].getOrElse(0)
  def height:Int = (raw \ "height").extractOpt[Int].getOrElse(0)
  def format:String = (raw \ "format").extractOpt[String].getOrElse(null)
}
case class LargeRawUploadResponse(public_id: String, url: String, secure_url: String, signature: String, bytes: Long,
  resource_type: String, tags: List[String] = List(), upload_id:Option[String], done:Option[Boolean]) extends VersionedResponse with TimestampedResponse
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
  requests: Int, resources: Int, derived_resources: Int) extends RawResponse {
  implicit val formats = DefaultFormats
  val nonAddonUsageKeys = Set("objects", "bandwidth", "storage")

  lazy val addonUsage: Map[String, UsageInfo] = raw match {
    case JObject(rawObject) => (for {
      (key: String, info: JObject) <- rawObject if !nonAddonUsageKeys.contains(key)
    } yield key -> info.extractOpt[UsageInfo]).filter(_._2.isDefined).map(p => p._1 -> p._2.get).toMap
    case _ => Map()
  }
}
case class ResourceTypesResponse(resource_types: List[String]) extends RawResponse
case class ResourcesResponse(next_cursor: Option[String]) extends RawResponse {
  implicit val formats = DefaultFormats

  lazy val resources: List[ResourceResponse] = (for {
    JArray(l) <- raw \ "resources"
    resourceRaw <- l
  } yield resourceRaw.extractOpt[ResourceResponse].map {
    rr =>
      rr.raw = resourceRaw;
      rr
  }).flatten
}
case class DeleteResourceResponse(deleted: Map[String, String], next_cursor: Option[String]) extends RawResponse
case class TransformationsResponse(transformations: List[TransformationInfo], next_cursor: Option[String]) extends RawResponse
case class TransformationResponse(name: String, allowed_for_strict: Boolean, used: Boolean, derived: List[DerivedInfo]) extends RawResponse {
  def info = parseTrasnsormation(raw \ "info")
}
case class TransformationUpdateResponse(message: String)

case class UploadPresetsResponse(presets: List[UnparsedUploadPreset], next_cursor: Option[String]) extends RawResponse
class UploadPresetResponse extends RawResponse {
  implicit val formats = DefaultFormats
  lazy val preset = 
  UploadPreset(
    (raw \ "name").extract[String], 
    (raw \ "unsigned").extract[Boolean], 
    UploadParameters(raw \ "settings" match {
      case JObject(values) => values.collect{
          case ("face_coordinates", value:JObject) => "face_coordinates" -> FacesInfo(value.extract[List[FaceInfo]])
          case ("transformation", v:JValue) => "transformation" -> parseTrasnsormation(v)
          case ("eager", JArray(transformations)) => "eager" -> transformations.map(parseTrasnsormation)
          case ("context", value:JObject) => "context" -> ContextMap(value.extract[Map[String, String]])
          case ("headers", value:JObject) => "headers" -> HeaderMap(value.extract[Map[String, String]])
          case (key, JArray(value)) => key -> StringSet(value.map{jv => jv.extract[String]}.toSet)
          case (key, value:JValue) => key -> value.values
        }.toMap
      case  _ => Map()
    }))
}
case class UploadPresetUpdateResponse(message: String)
case class UploadPresetCreateResponse(message: String, name:String)

case class TagsResponse(tags: List[String], next_cursor: Option[String]) extends RawResponse

case class FolderInfo(name:String, path:String)
case class FolderListResponse(folders: List[FolderInfo])

//Shared
case class ResourceResponse(public_id: String, url: String, secure_url: String, bytes: Int,
  width: Int, height: Int, format: String, resource_type: String, `type`: String,
  derived: List[DerivedTransformInfo] = List(), tags: List[String] = List()) extends AdvancedResponse with VersionedResponse with TimestampedResponse

trait RawResponse {
  private var rawJson: JsonAST.JValue = null
  private[cloudinary] def raw_=(json: JsonAST.JValue) = rawJson = json
  def raw = rawJson

  protected def parseTrasnsormation(t:JValue) = 
    Transformation(for {
        JArray(l) <- t
      } yield {
        (for {
          JObject(o) <- l
          (key, value) <- o
        } yield {
          key -> value.values
        }).toMap
      })
}

trait VersionedResponse extends RawResponse {
  lazy val version = raw \ "version" match {
    case JInt(v) => v.toString
    case JString(v) => v
    case v @ _ => v.toString
  }
}

trait TimestampedResponse extends RawResponse {
  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  lazy val createdAt = {
    (raw \ "created_at") match {
      case JString(s) => try {
        dateFormatter.parse(s)
      } catch {
        case _: Throwable => null
      }
      case _ => null
    }
  }

  lazy val uploadedAt = {
    (raw \ "uploaded_at") match {
      case JString(s) => try {
        dateFormatter.parse(s)
      } catch {
        case _: Throwable => null
      }
      case _ => null
    }
  }
}

trait AdvancedResponse extends RawResponse {
  implicit val formats = DefaultFormats + new EnumNameSerializer(ModerationStatus)
  
  lazy val eager: List[EagerInfo] = (for {
    JArray(l) <- raw \ "eager"
    v <- l
  } yield v.extract[EagerInfo])

  lazy val exif: Map[String, String] = (for {
    JObject(p) <- raw \ "exif"
    (k, JString(v)) <- p
  } yield (k -> v)).toMap

  lazy val metadata: Map[String, String] = (for {
    JObject(p) <- raw \ "image_metadata"
    (k, JString(v)) <- p
  } yield (k -> v)).toMap

  lazy val colors: List[ColorInfo] = for {
    JArray(l) <- raw \ "colors"
    JArray(JString(color) :: JDouble(rank) :: Nil) <- l
  } yield ColorInfo(color, rank)

  lazy val predominant: Map[String, List[ColorInfo]] = (for {
    JObject(i) <- raw \ "predominant"
    (k, JArray(v)) <- i
  } yield (k -> (for {
    JArray(JString(color) :: JDouble(rank) :: Nil) <- v
  } yield ColorInfo(color, rank)))).toMap

  lazy val faces: List[FaceInfo] = for {
    JArray(l) <- raw \ "faces"
    JArray(JInt(x) :: JInt(y) :: JInt(width) :: JInt(height) :: Nil) <- l
  } yield FaceInfo(x.toInt, y.toInt, width.toInt, height.toInt)

  lazy val customCoordinates: List[CustomCoordinate] = for {
    JArray(l) <- raw \ "coordinates" \ "custom"
    JArray(JInt(x) :: JInt(y) :: JInt(width) :: JInt(height) :: Nil) <- l
  } yield CustomCoordinate(x.toInt, y.toInt, width.toInt, height.toInt)

  lazy val context: Map[String, Map[String, String]] = (raw \ "context") match {
    case JObject(v) =>
      v.collect({ case (k, JObject(v)) => k -> v.collect({ case (ik, JString(iv)) => ik -> iv }).toMap }).toMap
    case _ => Map()
  }

  lazy val moderation: List[ModerationItem] = for {
    JArray(l) <- raw \ "moderation"
    v <- l
  } yield v.extract[ModerationItem]
  
  lazy val moderationStatus : Option[ModerationStatus.Value] = {
    val v = raw \ "moderation_status"
	v.extractOpt[ModerationStatus.Value]
  }

  lazy val pages:Int = (raw \ "pages").extractOpt[Int].getOrElse(1)
}

class ImageAnalysis(raw: JsonAST.JValue) {
  implicit lazy val formats = DefaultFormats

  lazy val faceCount: Int = (raw \ "face_count").extractOpt[Int].getOrElse(0)

  lazy val faces: List[FaceInfo] = for {
    JArray(l) <- raw \ "faces"
    JArray(JInt(x) :: JInt(y) :: JInt(width) :: JInt(height) :: Nil) <- l
  } yield FaceInfo(x.toInt, y.toInt, width.toInt, height.toInt)

  lazy val grayscale: Boolean = (raw \ "grayscale").extractOpt[Boolean].getOrElse(false)

  lazy val transparent: Boolean = (raw \ "transparent").extractOpt[Boolean].getOrElse(false)

  lazy val etag: Option[String] = (raw \ "etag").extractOpt[String]

  lazy val illustrationScore: Option[Float] = (raw \ "illustration_score").extractOpt[Float]

  lazy val colors: List[ColorInfo] = for {
    JArray(l) <- raw \ "colors"
    JArray(JString(color) :: JDouble(rank) :: Nil) <- l
  } yield ColorInfo(color, rank)
}

object SearchResourceResponse {
  implicit lazy val formats = DefaultFormats +
    new EnumNameSerializer(Status) +
    new EnumNameSerializer(ModerationStatus) +
    new EnumNameSerializer(AccessMode)
}

case class SearchResourceResponse(override val raw: JsonAST.JValue) extends VersionedResponse with TimestampedResponse {

  import SearchResourceResponse.formats

  lazy val publicId: String = (raw \ "public_id").extract[String]

  lazy val folder: String = (raw \ "folder").extract[String]

  lazy val filename: String = (raw \ "filename").extract[String]

  lazy val format: String = (raw \ "format").extract[String]

  lazy val resourceType: String = (raw \ "resource_type").extract[String]

  lazy val `type`: String = (raw \ "type").extract[String]

  lazy val bytes: Long = (raw \ "bytes").extract[Long]

  lazy val backupBytes: Long = (raw \ "backup_bytes").extract[Long]

  lazy val width: Int = (raw \ "width").extractOpt[Int].getOrElse(0)

  lazy val height: Int = (raw \ "height").extractOpt[Int].getOrElse(0)

  lazy val duration: Option[Float] = (raw \ "duration").extractOpt[Float]

  lazy val pages: Int = (raw \ "pages").extractOpt[Int].getOrElse(1)

  lazy val tags: Set[String] = (raw \ "tags").extractOpt[Array[String]].map(_.toSet).getOrElse(Set.empty)

  lazy val context: Map[String, String] = (raw \ "context").extractOpt[Map[String, String]].getOrElse(Map.empty)

  lazy val url: String = (raw \ "url").extract[String]

  lazy val secureUrl: String = (raw \ "secure_url").extract[String]

  lazy val status: Status.Value = (raw \ "status").extract[Status.Value]

  lazy val moderationStatus: ModerationStatus.Value = (raw \ "moderation_status").extract[ModerationStatus.Value]

  lazy val accessMode: AccessMode.Value = (raw \ "access_mode").extract[AccessMode.Value]

  lazy val imageMetadata: Map[String, String] = (raw \ "image_metadata").extractOpt[Map[String, String]].getOrElse(Map.empty)

  lazy val imageAnalysis: Option[ImageAnalysis] = raw \ "image_analysis" match {
    case v@JObject(_) => Some(new ImageAnalysis(v))
    case _ => None
  }
}

class SearchResponse extends RawResponse {
  implicit lazy val formats = DefaultFormats

  lazy val totalCount: Int = (raw \ "total_count").extract[Int]

  lazy val time: Long = (raw \ "time").extract[Long]

  lazy val aggregations: Option[Map[String, Long]] = (raw \ "aggregations").extractOpt[Map[String, Long]]

  lazy val nextCursor: Option[String] = (raw \ "next_cursor").extractOpt[String]

  lazy val resources: Iterable[SearchResourceResponse] = (raw \ "resources").extract[List[JsonAST.JValue]].map(SearchResourceResponse.apply)
}
