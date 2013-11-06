package com.cloudinary

import java.io.File
import java.io.IOException

import scala.concurrent.Future

import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import com.ning.http.client.RequestBuilder
import com.ning.http.multipart.ByteArrayPartSource
import com.ning.http.multipart.FilePart
import com.ning.http.multipart.StringPart

import com.cloudinary.response._
import com.cloudinary.parameters._

class Uploader(implicit val cloudinary: Cloudinary) {
  
  def signRequestParams(params: Map[String, Any]) = {
    val paramsAndTimeStamp = params + ("timestamp" -> (System.currentTimeMillis() / 1000L).toLong.toString)
    cloudinary.signRequest(paramsAndTimeStamp)
  }
  
  def createRequest(action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image") = {
    val params = Util.definedMap(optionalParams)
    
    val signedParams = signRequestParams(params)

    val apiUrl = cloudinary.cloudinaryApiUrl(action, resourceType)

    val apiUrlBuilder = new RequestBuilder("POST")
    apiUrlBuilder.setUrl(apiUrl)

    signedParams foreach {
      param =>
        val (k, v) = param
        v match {
          case list: Iterable[_] => list.foreach {
            v =>
              apiUrlBuilder.addBodyPart(
                new StringPart(k + "[]", v.toString, "UTF-8"))
          }
          case null =>
          case _ => apiUrlBuilder.addBodyPart(new StringPart(k, v.toString, "UTF-8"))
        }
    }

    file match {
      case fp: FilePart => apiUrlBuilder.addBodyPart(fp)
      case f: File => apiUrlBuilder.addBodyPart(new FilePart("file", f))
      case fn: String if !fn.matches(illegalFileName) => apiUrlBuilder.addBodyPart(new FilePart("file", new File(fn)))
      case body: String => apiUrlBuilder.addBodyPart(new StringPart("file", body, "UTF-8"))
      case body: Array[Byte] => apiUrlBuilder.addBodyPart(new FilePart("file", new ByteArrayPartSource("file", body)))
      case null =>
      case _ => throw new IOException("Uprecognized file parameter " + file);
    }
    
    apiUrlBuilder.build()
  }

  def callApi[T](action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image")(implicit mf: scala.reflect.Manifest[T]): Future[T] = {
	val request = createRequest(action, optionalParams, file, resourceType)
    HttpClient.executeAndExtractResponse[T](request)
  }
  
  def callRawApi(action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image") = {
	val request = createRequest(action, optionalParams, file, resourceType)
    HttpClient.executeCloudinaryRequest(request)
  }

  def callTagsApi(tag: String, command: String, publicIds: Iterable[String], `type`: Option[String]) = {
    val params = Map(
      "tag" -> tag,
      "command" -> command,
      "type" -> `type`,
      "public_ids" -> publicIds.toList)
    callApi[TagResponse]("tags", params, null);
  }

  def upload(file: AnyRef, params: UploadParameters = UploadParameters(), resourceType: String = "image") = {
    callApi[UploadResponse]("upload", params.toMap, file, resourceType)
  }

  def destroy(publicId: String, resourceType: String = "image", `type`: Option[String] = None, invalidate: Option[Boolean] = None) = {
    val params = Map(
      "type" -> `type`,
      "public_id" -> publicId,
      "invalidate" -> invalidate)
    callApi[DestroyResponse]("destroy", params, null, resourceType)
  }

  def rename(fromPublicId: String, toPublicId: String, overwrite: Option[Boolean] = None, `type`: Option[String] = None) = {
    val params = Map(
      "type" -> `type`,
      "overwrite" -> overwrite,
      "from_public_id" -> fromPublicId,
      "to_public_id" -> toPublicId)
    callApi[ResourceResponse]("rename", params, null)
  }

  def explicit(publicId: String, callback:Option[String] = None, `type`:Option[String] = None, 
      eager:List[Transformation] = List(), customHeaders:Map[String, String] = Map(), tags:List[String] = List()) = {
    val params = UploadParameters(
        publicId = Some(publicId),
        callback = callback,
        `type` = `type`,
        eager = eager,
        customHeaders = customHeaders,
        tags = tags)
    callApi[ExplicitResponse]("explicit", params.toMap, null)
  }

  def generateSprite(tag: String, transformation: Option[Transformation] = None,
    format: Option[String] = None, notificationUrl: Option[String] = None, async: Option[Boolean] = None) = {

    val formattedTransformation = format match {
      case Some(f: String) => transformation.map(_.fetchFormat(f))
      case _ => transformation
    }

    val params = Map(
      "transformation" -> formattedTransformation.map(_.generate()),
      "tag" -> tag,
      "notification_url" -> notificationUrl,
      "async" -> async)
    callApi[SpriteResponse]("sprite", params, null);
  }

  def multi(tag: String, transformation: Option[Transformation] = None,
    format: Option[String] = None, notificationUrl: Option[String] = None, async: Option[Boolean] = None) = {
    val params = Map(
      "transformation" -> transformation.map(_.generate()),
      "tag" -> tag,
      "notification_url" -> notificationUrl,
      "async" -> async,
      "format" -> format)
    callApi[MultiResponse]("multi", params, null)
  }

  def explode(publicId: String, transformation: Option[Transformation] = None,
    format: Option[String] = None, notificationUrl: Option[String] = None) = {
    val params = Map(
      "transformation" -> transformation.map(_.generate),
      "public_id" -> publicId,
      "notification_url" -> notificationUrl,
      "format" -> format)
    callApi[ExplodeResponse]("explode", params, null);
  }

  // options may include 'exclusive' (boolean) which causes clearing this tag
  // from all other resources
  def addTag(tag: String, publicIds: Iterable[String], exclusive: Boolean = false, `type`: Option[String] = None) = {
    val command = if (exclusive) "set_exclusive" else "add"
    callTagsApi(tag, command, publicIds, `type`);
  }

  def removeTag(tag: String, publicIds: Iterable[String], `type`: Option[String] = None) = {
    callTagsApi(tag, "remove", publicIds, `type`);
  }

  def replaceTag(tag: String, publicIds: Iterable[String], `type`: Option[String] = None) = {
    callTagsApi(tag, "replace", publicIds, `type`);
  }

  def text(params:TextParameters) = {
    callApi[TextResponse]("text", params.toMap, null);
  }

  private def escapeHtml(s: Any) = xml.Utility.escape(s.toString)

  def imageUploadTag(field: String, uploadParameters:UploadParameters, paramHtmlOptions: Map[String, Any] = Map(), resourceType:String = "image") = {
    val htmlOptions = if (paramHtmlOptions == null) Map[String, Any]() else paramHtmlOptions
    val htmlOptionsString = htmlOptions
      .filterNot(p => p._2 == null || p._1 == "class")
      .mapValues(escapeHtml)
      .map(p => { p._1 } + "=\"" + p._2 + "\"").mkString(" ")
    val classes = ("cloudinary-fileupload" :: htmlOptions.filter(_._1 == "class").values.toList).map(escapeHtml).mkString(" ")
    val tagParams = escapeHtml(uploadTagParams(uploadParameters.toMap, resourceType))
    val cloudinaryUploadUrl = cloudinary.cloudinaryApiUrl("upload", resourceType)

    s"""
<input type="file" name="file" 
		data-url="$cloudinaryUploadUrl" 
		data-form-data="$tagParams" 
		data-cloudinary-field="$field" 
		class="$classes"
		$htmlOptionsString/>
"""
  }
  
  protected def uploadTagParams(paramOptions: Map[String, Any] = Map(), resourceType: String = "image") = {
    var options = paramOptions

    val callback = options.get("callback") match {
      case None => cloudinary.getStringConfig("callback")
      case c @ _ => c
    }
    if (callback.isEmpty)
      throw new IllegalArgumentException("Must supply callback")

    options = options + ("callback" -> callback.get)

    val params = Util.definedMap(options)
    val signedParams = signRequestParams(params)

    compact(render(signedParams.mapValues(_.toString) + ("resource_type" -> resourceType)))
  }
  
  private[cloudinary] val illegalFileName = "https?:.*|s3:.*|data:[^;]*;base64,([a-zA-Z0-9/+\n=]+)"

}
