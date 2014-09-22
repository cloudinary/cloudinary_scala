package com.cloudinary

import java.util.Date
import java.util.TimeZone
import scala.concurrent.Future
import com.ning.http.client.Realm
import com.ning.http.client.RequestBuilder
import response._
import parameters.UpdateParameters
import java.text.SimpleDateFormat

object Api {
  abstract class HttpMethod(val method: String);
  case object GET extends HttpMethod("GET")
  case object POST extends HttpMethod("POST")
  case object PUT extends HttpMethod("PUT")
  case object DELETE extends HttpMethod("DELETE")
  
  abstract class ListDirection(val dir:String)
  case object ASCENDING extends ListDirection("asc")
  case object DESCENDING extends ListDirection("desc")
}

class Api(implicit cloudinary: Cloudinary) {
  
  def createRequest(
    method: Api.HttpMethod,
    uri: Iterable[String],
    params: Map[String, Any]) = {
    val apiUrl: String =
      (cloudinary.cloudinaryApiUrlPrefix() :: uri.toList).filterNot(_.isEmpty()).mkString("/")

    val apiUrlBuilder = new RequestBuilder(method.method).setUrl(apiUrl)
    for (param <- Util.definedMap(params)) {
      val (k, v) = param
      if (v.isInstanceOf[Iterable[_]]) {
        for (sv <- v.asInstanceOf[Iterable[String]]) {
          method match {
            case Api.GET => apiUrlBuilder.addQueryParameter(k + "[]", sv)
            case _ => apiUrlBuilder.addParameter(k + "[]", sv)
          }
        }
      } else {
        method match {
          case Api.GET => apiUrlBuilder.addQueryParameter(k, v.toString)
          case _ => apiUrlBuilder.addParameter(k, v.toString)
        }
      }
    }

    val realm = new Realm.RealmBuilder()
      .setPrincipal(cloudinary.apiKey())
      .setPassword(cloudinary.apiSecret())
      .setUsePreemptiveAuth(true)
      .setScheme(Realm.AuthScheme.BASIC)
      .build()

    apiUrlBuilder.setRealm(realm).build()
  }
  
  def callApi[T](
    method: Api.HttpMethod,
    uri: Iterable[String],
    params: Map[String, Any])(implicit mf: scala.reflect.Manifest[T]): Future[T] = {
    val request = createRequest(method, uri, params)
    HttpClient.executeAndExtractResponse[T](request)
  }
  
  def callApiRaw(
    method: Api.HttpMethod,
    uri: Iterable[String],
    params: Map[String, Any]) = {
    val request = createRequest(method, uri, params)
    HttpClient.executeCloudinaryRequest(request)
  }

  def ping() =
    callApi[PingResponse](Api.GET, "ping" :: Nil, Map())

  def usage() =
    callApi[UsageResponse](Api.GET, "usage" :: Nil, Map())
  
  def resourceTypes() =
    callApi[ResourceTypesResponse](Api.GET, "resources" :: Nil, Map())

  def resources(nextCursor: Option[String] = None, maxResults: Option[Int] = None, prefix: Option[String] = None, 
      tags: Boolean = false, context: Boolean = false, moderations: Boolean = false,
    direction: Option[Api.ListDirection] = None, resourceType: String = "image", `type`: Option[String] = None, startAt: Option[Date] = None) =
    callApi[ResourcesResponse](Api.GET, "resources" :: resourceType :: `type`.getOrElse("") :: Nil,
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults, "prefix" -> prefix, 
          "tags" -> tags, "context" -> context, "moderations" -> moderations, 
          "direction" -> direction.map(_.dir), "start_at" -> startAt.map{d => gmtDateFormat.format(d) + " GMT"}))

  def resourcesByTag(tag: String, nextCursor: Option[String] = None, maxResults: Option[Int] = None, resourceType: String = "image", 
      direction: Option[Api.ListDirection] = None, tags: Boolean = false, context: Boolean = false, moderations: Boolean = false) = {
    callApi[ResourcesResponse](Api.GET, List("resources", resourceType, "tags", tag),
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults, "tags" -> tags, "context" -> context, "moderations" -> moderations, "direction" -> direction.map(_.dir)))
  }
  
  def resourcesByIds(publicIds:List[String], tags: Boolean = false, context: Boolean = false, moderations: Boolean = false,
    resourceType: String = "image", `type`: String = "upload") =
    callApi[ResourcesResponse](Api.GET, "resources" :: resourceType :: `type` :: Nil,
      Map("public_ids" -> publicIds.mkString(","), "tags" -> tags, "context" -> context, "moderations" -> moderations))
  
  def resourcesByModeration(status:ModerationStatus.Value, kind:String = "manual", 
      tags: Boolean = false, context: Boolean = false, moderations: Boolean = false,
      nextCursor: Option[String] = None, maxResults: Option[Int] = None, direction: Option[Api.ListDirection] = None,
      resourceType: String = "image") =
    callApi[ResourcesResponse](Api.GET, "resources" :: resourceType :: "moderations" :: kind :: status.toString :: Nil,
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults, "tags" -> tags, "context" -> context, "moderations" -> moderations, "direction" -> direction.map(_.dir)))    

  def resource(publicId: String, derived:Boolean = true, exif: Boolean = false, colors: Boolean = false, 
    faces: Boolean = false, coordinates: Boolean = false, imageMetadata: Boolean = false,
    pages: Boolean = false, maxResults: Option[Int] = None, resourceType: String = "image", `type`: String = "upload") = {
    callApi[ResourceResponse](Api.GET, List("resources", resourceType, `type`, publicId),
      Map("derived" -> derived, "exif" -> exif, "colors" -> colors, "faces" -> faces, "coordinates" -> true, 
          "image_metadata" -> imageMetadata, "pages" -> pages, "max_results" -> maxResults));
  }
  
  def update(publicId: String, parameters:UpdateParameters, resourceType: String = "image", `type`: String = "upload") = {
    callApi[ResourceResponse](Api.POST, List("resources", resourceType, `type`, publicId), parameters.toMap);
  }

  def deleteResources(publicIds: Iterable[String], nextCursor:Option[String] = None, keepOriginal: Boolean = false, 
                      invalidate: Boolean = false, resourceType: String = "image", `type`: String = "upload") =
    callApi[DeleteResourceResponse](Api.DELETE, List("resources", resourceType, `type`),
      Map("public_ids" -> publicIds, "keep_original" -> keepOriginal, "next_cursor" -> nextCursor, "invalidate" -> invalidate))

  def deleteResourcesByPrefix(prefix: String, nextCursor:Option[String] = None, keepOriginal: Boolean = false, 
                              invalidate: Boolean = false, resourceType: String = "image", `type`: String = "upload") =
    callApi[DeleteResourceResponse](Api.DELETE, List("resources", resourceType, `type`),
      Map("prefix" -> prefix, "keep_original" -> keepOriginal, "next_cursor" -> nextCursor, "invalidate" -> invalidate))

  def deleteResourcesByTag(tag: String, nextCursor:Option[String] = None, keepOriginal: Boolean = false, 
                           invalidate: Boolean = false, resourceType: String = "image") =
    callApi[DeleteResourceResponse](Api.DELETE, List("resources", resourceType, "tags", tag),
      Map("keep_original" -> keepOriginal, "next_cursor" -> nextCursor, "invalidate" -> invalidate))
      
  def deleteAllResources(nextCursor:Option[String] = None, keepOriginal: Boolean = false, 
                         invalidate: Boolean = false, resourceType: String = "image", `type`: String = "upload") =
    callApi[DeleteResourceResponse](Api.DELETE, List("resources", resourceType, `type`),
      Map("all" -> true, "keep_original" -> keepOriginal, "next_cursor" -> nextCursor, "invalidate" -> invalidate))

  def deleteDerivedResources(derivedResourceIds: Iterable[String], options: Map[String, Any] = Map()) =
    callApi[DeleteResourceResponse](Api.DELETE, List("derived_resources"),
      Map("derived_resource_ids" -> derivedResourceIds))

  def tags(nextCursor: Option[String] = None, maxResults: Option[Int] = None, prefix: Option[String] = None, resourceType: String = "image") =
    callApi[TagsResponse](Api.GET, "tags" :: resourceType :: Nil,
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults, "prefix" -> prefix))

  def transformations(nextCursor: Option[String] = None, maxResults: Option[Int] = None) = {
    callApi[TransformationsResponse](Api.GET, "transformations" :: Nil,
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults))
  }
  def transformationByName(t:String, maxResults: Option[Int] = None):Future[TransformationResponse] =
    callApi[TransformationResponse](Api.GET, "transformations" :: t :: Nil,
      Map("max_results" -> maxResults))
      
  def transformation(t: Transformation, maxResults: Option[Int] = None):Future[TransformationResponse] =
    transformationByName(t.generate, maxResults)
      
  def deleteTransformation(transformation: String):Future[TransformationUpdateResponse] = {
    callApi[TransformationUpdateResponse](Api.DELETE, "transformations" :: transformation :: Nil, Map());
  }
  
  def deleteTransformation(t: Transformation):Future[TransformationUpdateResponse] = deleteTransformation(t.generate)

  // updates - currently only supported update are:
  // "allowed_for_strict": boolean flag
  // "unsafe_update": transformation string
  def updateTransformation(transformation: String, allowedForStrict:Option[Boolean] = None, unsafeUpdate:Option[Transformation] = None) = 
    callApi[TransformationUpdateResponse](Api.PUT, "transformations" :: transformation :: Nil, 
        Map("allowed_for_strict" -> allowedForStrict, "unsafe_update" -> unsafeUpdate.map(_.generate)));

  def createTransformation(name: String, definition: Transformation) = {
    callApi[TransformationUpdateResponse](Api.POST, "transformations" :: name :: Nil, Map("transformation" -> definition.generate));
  }
  
  def uploadPresets(nextCursor: Option[String] = None, maxResults: Option[Int] = None) = {
    callApi[UploadPresetsResponse](Api.GET, "upload_presets" :: Nil,
      Map("next_cursor" -> nextCursor, "max_results" -> maxResults))
  }
  
  def uploadPreset(name:String, maxResults: Option[Int] = None) = {
    callApi[UploadPresetResponse](Api.GET, "upload_presets" :: name :: Nil,
      Map("max_results" -> maxResults))
  }

  def deleteUploadPreset(name:String) = {
    callApi[UploadPresetUpdateResponse](Api.DELETE, "upload_presets" :: name :: Nil, Map())
  }

  def createUploadPreset(uploadPreset:UploadPreset) = {
    callApi[UploadPresetCreateResponse](Api.POST, "upload_presets" :: Nil,
      uploadPreset.toMap)
  }

  def updateUploadPreset(uploadPreset:UploadPreset) = {
    callApi[UploadPresetUpdateResponse](Api.PUT, "upload_presets" :: uploadPreset.name :: Nil,
      uploadPreset.toMap.filterKeys{_ != "name"})
  }
  
  private lazy val gmtDateFormat = {
    val df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    df
  }

}
