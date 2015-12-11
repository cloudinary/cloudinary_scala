package com.cloudinary

import java.io._

import scala.concurrent.Future

import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import com.ning.http.client.RequestBuilder
import com.ning.http.client.multipart.ByteArrayPart
import com.ning.http.client.multipart.FilePart
import com.ning.http.client.multipart.StringPart

import com.cloudinary.response._
import com.cloudinary.parameters._

import concurrent.ExecutionContext.Implicits.global

class Uploader(implicit val cloudinary: Cloudinary) {

  def signRequestParams(params: Map[String, Any]) = {
    val paramsAndTimeStamp = params + ("timestamp" -> (System.currentTimeMillis() / 1000L).toLong.toString)
    cloudinary.signRequest(paramsAndTimeStamp)
  }

  def createRequest(action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image", signed:Boolean = true) = {
    val params = Util.definedMap(optionalParams)

    val processedParams = if (signed) signRequestParams(params) else params

    val apiUrl = cloudinary.cloudinaryApiUrl(action, resourceType)

    val apiUrlBuilder = new RequestBuilder("POST")
    apiUrlBuilder.setUrl(apiUrl)

    processedParams foreach {
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
      case baps:ByteArrayPart => apiUrlBuilder.addBodyPart(baps)
      case fp: FilePart => apiUrlBuilder.addBodyPart(fp)
      case f: File => apiUrlBuilder.addBodyPart(new FilePart("file", f))
      case fn: String if !fn.matches(illegalFileName) => apiUrlBuilder.addBodyPart(new FilePart("file", new File(fn)))
      case body: String => apiUrlBuilder.addBodyPart(new StringPart("file", body, "UTF-8"))
      case body: Array[Byte] => apiUrlBuilder.addBodyPart(new ByteArrayPart("file", body))
      case null =>
      case _ => throw new IOException("Uprecognized file parameter " + file);
    }

    apiUrlBuilder.build()
  }

  def callApi[T](action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image", signed:Boolean = true)(implicit mf: scala.reflect.Manifest[T]): Future[T] = {
    val request = createRequest(action, optionalParams, file, resourceType, signed)
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

  def unsignedUpload(file: AnyRef, uploadPreset:String, params: UploadParameters = UploadParameters(), resourceType: String = "image") = {
    upload(file, params.uploadPreset(uploadPreset).copy(signed = false), resourceType)
  }

  def upload(file: AnyRef, params: UploadParameters = UploadParameters(), resourceType: String = "image") = {
    callApi[UploadResponse]("upload", params.toMap, file, resourceType, params.signed)
  }

  def uploadLargeRaw(file: AnyRef, params: LargeUploadParameters = LargeUploadParameters(), bufferSize: Int = 20000000) = {
    val (input, fileName) = file match {
      case s: String =>
       	val f = new File(s)
        (new FileInputStream(f), Some(f.getName))
      case f: File => (new FileInputStream(f), Some(f.getName))
      case b: Array[Byte] => (new ByteArrayInputStream(b), None)
      case is: InputStream => (is, None)
    }
    try {
    	uploadLargeRawPart(input, params, fileName, bufferSize)
    } catch {
      case e:Exception =>
        input.close()
        throw e
    }
  }

  private def uploadLargeRawPart(input: InputStream, params: LargeUploadParameters, originalFileName:Option[String], bufferSize: Int, partNumber: Int = 1): Future[LargeRawUploadResponse] = {
    val uploadParams = params.toMap + ("part_number" -> partNumber.toString)
    val (last, buffer) = readChunck(input, bufferSize)
    val part = new ByteArrayPart(originalFileName.getOrElse("file"), buffer)
    (partNumber, last) match {
      case (_, true) =>
        input.close()
        callApi[LargeRawUploadResponse]("upload_large", uploadParams + ("final" -> "1"), part, "raw")
      case (1, _) =>
        val responseFuture = callApi[LargeRawUploadResponse]("upload_large", uploadParams, part, "raw")
        responseFuture.flatMap { response =>
          uploadLargeRawPart(input, params.publicId(response.public_id).uploadId(response.upload_id.get), originalFileName, bufferSize, partNumber + 1)
        }
      case _ =>
        val responseFuture = callApi[LargeRawUploadResponse]("upload_large", uploadParams, part, "raw")
        responseFuture.flatMap{
          response => uploadLargeRawPart(input, params, originalFileName, bufferSize, partNumber + 1)
        }
    }
  }
  
  private def readChunck(input: InputStream, bufferSize: Int) = {
    val buffer = new Array[Byte](bufferSize)
    var bytesWritten = 0
    var bytesRead = 0
    var loop = true 
    do {
    	bytesRead = input.read(buffer, bytesWritten, bufferSize - bytesWritten)
    	loop = (bytesRead != -1 && bytesRead + bytesWritten < bufferSize)
    	bytesWritten = bytesWritten + math.max(bytesRead, 0)
    } while(loop)
    (bytesRead == -1, buffer.take(bytesWritten))
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

  def explicit(publicId: String, callback: Option[String] = None, `type`: Option[String] = None,
    eager: List[Transformation] = List(), customHeaders: Map[String, String] = Map(), tags: Set[String] = Set(), faceCoordinates: List[FaceInfo] = List()) = {
    val params = UploadParameters().publicId(publicId).`type`(`type`.getOrElse("")).eager(eager).
      headers(customHeaders).tags(tags).faceCoordinates(faceCoordinates).
      callback(callback.getOrElse(""))
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

  def text(params: TextParameters) = {
    callApi[TextResponse]("text", params.toMap, null);
  }

  private def escapeHtml(s: Any) = xml.Utility.escape(s.toString)

  def unsignedImageUploadTag(field: String, preset:String, uploadParameters: UploadParameters = UploadParameters(), paramHtmlOptions: Map[String, Any] = Map(), resourceType: String = "image") = 
    imageUploadTag(field, uploadParameters.uploadPreset(preset).copy(signed = false), paramHtmlOptions, resourceType)

  def imageUploadTag(field: String, uploadParameters: UploadParameters = UploadParameters(), paramHtmlOptions: Map[String, Any] = Map(), resourceType: String = "image") = {
    val htmlOptions = if (paramHtmlOptions == null) Map[String, Any]() else paramHtmlOptions
    val htmlOptionsString = htmlOptions
      .filterNot(p => p._2 == null || p._1 == "class")
      .mapValues(escapeHtml)
      .map(p => { p._1 } + "=\"" + p._2 + "\"").mkString(" ")
    val classes = ("cloudinary-fileupload" :: htmlOptions.filter(_._1 == "class").values.toList).map(escapeHtml).mkString(" ")
    val tagParams = escapeHtml(uploadTagParams(uploadParameters.toMap, resourceType, uploadParameters.signed))
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

  protected def uploadTagParams(paramOptions: Map[String, Any] = Map(), resourceType: String = "image", signed:Boolean = true) = {
    var options = paramOptions

    val callback = options.get("callback") match {
      case None => cloudinary.getStringConfig("callback")
      case c @ _ => c
    }
    if (callback.isEmpty)
      throw new IllegalArgumentException("Must supply callback")

    options = options + ("callback" -> callback.get)

    val params = Util.definedMap(options)
    val processedParams = if (signed) signRequestParams(params) else params

    compact(render(processedParams.mapValues(_.toString) + ("resource_type" -> resourceType)))
  }

  private[cloudinary] val illegalFileName = "https?:.*|s3:.*|data:[^;]*;base64,([a-zA-Z0-9/+\n=]+)"

}
