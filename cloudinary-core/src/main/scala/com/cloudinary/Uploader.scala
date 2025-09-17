package com.cloudinary

import java.io._
import java.nio.charset.StandardCharsets

import com.cloudinary.parameters._
import com.cloudinary.response._
import com.ning.http.client.RequestBuilder
import com.ning.http.client.multipart.{ByteArrayPart, FilePart, StringPart}
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Uploader(implicit val cloudinary: Cloudinary) {

  private[cloudinary] val httpclient: HttpClient = new HttpClient

  def signRequestParams(params: Map[String, Any]) = {
    val paramsAndTimeStamp = params + ("timestamp" -> (System.currentTimeMillis() / 1000L).toLong.toString)
    cloudinary.signRequest(paramsAndTimeStamp)
  }

  def createRequest(action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image", signed: Boolean = true, requestTimeout: Option[Int] = None) = {
    val params = Util.definedMap(optionalParams)

    val processedParams = if (signed) signRequestParams(params) else params

    val apiUrl = cloudinary.cloudinaryApiUrl(action, resourceType)

    val apiUrlBuilder = new RequestBuilder("POST")
    apiUrlBuilder.setUrl(apiUrl)
    requestTimeout.map(timeout => apiUrlBuilder.setRequestTimeout(timeout)) //in milliseconds

    processedParams foreach {
      param =>
        val (k, v) = param
        v match {
          case list: Iterable[_] => list.foreach {
            v =>
              apiUrlBuilder.addBodyPart(
                new StringPart(k + "[]", v.toString, StringPart.DEFAULT_CONTENT_TYPE, StandardCharsets.UTF_8))
          }
          case null =>
          case _ => apiUrlBuilder.addBodyPart(new StringPart(k, v.toString, StringPart.DEFAULT_CONTENT_TYPE, StandardCharsets.UTF_8))
        }
    }

    file match {
      case baps: ByteArrayPart => apiUrlBuilder.addBodyPart(baps)
      case fp: FilePart => apiUrlBuilder.addBodyPart(fp)
      case f: File => apiUrlBuilder.addBodyPart(new FilePart("file", f))
      case fn: String if !fn.matches(illegalFileName) => apiUrlBuilder.addBodyPart(new FilePart("file", new File(fn)))
      case body: String => apiUrlBuilder.addBodyPart(new StringPart("file", body, StringPart.DEFAULT_CONTENT_TYPE, StandardCharsets.UTF_8))
      case body: Array[Byte] => apiUrlBuilder.addBodyPart(new ByteArrayPart("file", body))
      case null =>
      case _ => throw new IOException("Unrecognized file parameter " + file);
    }

    apiUrlBuilder.build()
  }

  def callApi[T](action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image", signed: Boolean = true, requestTimeout: Option[Int] = None)
                (implicit mf: scala.reflect.Manifest[T]): Future[T] = {
    val request = createRequest(action, optionalParams, file, resourceType, signed, requestTimeout)
    httpclient.executeAndExtractResponse[T](request)
  }

  def callApiWithHeaders[T](action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image", headers: Map[String, String] = Map(), signed: Boolean = true, requestTimeout: Option[Int] = None)
                           (implicit mf: scala.reflect.Manifest[T]): Future[T] = {
    val request = createRequest(action, optionalParams, file, resourceType, signed, requestTimeout)
    // Add custom headers to the request
    headers.foreach { case (key, value) =>
      request.getHeaders.add(key, value)
    }
    httpclient.executeAndExtractResponse[T](request)
  }

  def callRawApi(action: String, optionalParams: Map[String, Any], file: AnyRef, resourceType: String = "image") = {
    val request = createRequest(action, optionalParams, file, resourceType)
    httpclient.executeCloudinaryRequest(request)
  }

  def callTagsApi(tag: String, command: String, publicIds: Iterable[String], `type`: Option[String]) = {
    val params = Map(
      "tag" -> tag,
      "command" -> command,
      "type" -> `type`,
      "public_ids" -> publicIds.toList)
    callApi[TagResponse]("tags", params, null);
  }

  def unsignedUpload(file: AnyRef, uploadPreset: String, params: UploadParameters = UploadParameters(), resourceType: String = "image", requestTimeout: Option[Int] = None) = {
    upload(file, params.uploadPreset(uploadPreset).copy(signed = false), resourceType, requestTimeout)
  }

  def upload(file: AnyRef, params: UploadParameters = UploadParameters(), resourceType: String = "image", requestTimeout: Option[Int] = None) = {
    callApi[UploadResponse]("upload", params.toMap, file, resourceType, params.signed, requestTimeout)
  }

  def uploadLarge(file: AnyRef, params: LargeUploadParameters = LargeUploadParameters(), resourceType: String = "image", chunkSize: Int = 20000000): Future[UploadResponse] = {
    // Handle remote URLs - delegate to regular upload
    file match {
      case url: String if url.startsWith("http://") || url.startsWith("https://") =>
        // For URLs, call API directly
        callApi[UploadResponse]("upload", params.toMap, url, resourceType, params.signed)
      case _ =>
        val (input, fileName, fileSize) = file match {
          case s: String =>
            val f = new File(s)
            (new FileInputStream(f), Some(f.getName), f.length())
          case f: File =>
            (new FileInputStream(f), Some(f.getName), f.length())
          case b: Array[Byte] =>
            (new ByteArrayInputStream(b), None, b.length.toLong)
          case is: InputStream =>
            // For InputStreams, we can't determine size upfront - read all data
            val buffer = scala.io.Source.fromInputStream(is, "ISO-8859-1").map(_.toByte).toArray
            is.close()
            (new ByteArrayInputStream(buffer), None, buffer.length.toLong)
        }

        uploadLargeWithRanges(input, fileName, fileSize, params, resourceType, chunkSize)
    }
  }

  def uploadLargeRaw(file: AnyRef, params: LargeUploadParameters = LargeUploadParameters(), chunkSize: Int = 20000000): Future[LargeRawUploadResponse] = {
    // Backwards compatibility wrapper - delegate to generic uploadLarge and convert response
    uploadLarge(file, params, "raw", chunkSize).map { uploadResponse =>
      val response = LargeRawUploadResponse(
        public_id = uploadResponse.public_id,
        url = uploadResponse.url,
        secure_url = uploadResponse.secure_url,
        signature = uploadResponse.signature,
        bytes = uploadResponse.bytes,
        resource_type = uploadResponse.resource_type
      )
      response.raw = uploadResponse.raw
      response
    }
  }

  private def uploadLargeWithRanges(input: InputStream, fileName: Option[String], fileSize: Long, params: LargeUploadParameters, resourceType: String, chunkSize: Int): Future[UploadResponse] = {
    // Generate unique upload ID for this upload session
    val uploadId = Cloudinary.randomPublicId()
    var currentLoc = 0L
    var uploadResult: Future[UploadResponse] = null
    var updatedParams = params

    val finalFileName = fileName.getOrElse("stream")

    def uploadNextChunk(): Future[UploadResponse] = {
      val buffer = new Array[Byte](chunkSize)
      val bytesRead = input.read(buffer)

      if (bytesRead <= 0) {
        // No more data, close stream and return the last result
        try { input.close() } catch { case _: Exception => }
        uploadResult
      } else {
        val actualBuffer = if (bytesRead < chunkSize) {
          buffer.take(bytesRead)
        } else {
          buffer
        }

        val contentRange = s"bytes $currentLoc-${currentLoc + bytesRead - 1}/$fileSize"
        currentLoc += bytesRead

        // Create multipart with HTTP headers
        val part = new ByteArrayPart("file", actualBuffer, null, null, finalFileName)

        // HTTP headers should not be included in signature parameters
        val uploadParams = updatedParams.toMap

        val responseFuture = callApiWithHeaders[UploadResponse]("upload", uploadParams, part, resourceType,
          Map("Content-Range" -> contentRange, "X-Unique-Upload-Id" -> uploadId), updatedParams.signed)

        responseFuture.flatMap { response =>
          uploadResult = responseFuture
          // Update params with public_id from response for subsequent chunks
          updatedParams = updatedParams.publicId(response.public_id)

          // Check if there's more data to upload
          if (input.available() > 0) {
            uploadNextChunk()
          } else {
            // Close the stream when upload is complete
            try { input.close() } catch { case _: Exception => }
            Future.successful(response)
          }
        }
      }
    }

    uploadNextChunk()
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
    } while (loop)
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

  def unsignedImageUploadTag(field: String, preset: String, uploadParameters: UploadParameters = UploadParameters(), paramHtmlOptions: Map[String, Any] = Map(), resourceType: String = "image") =
    imageUploadTag(field, uploadParameters.uploadPreset(preset).copy(signed = false), paramHtmlOptions, resourceType)

  def imageUploadTag(field: String, uploadParameters: UploadParameters = UploadParameters(), paramHtmlOptions: Map[String, Any] = Map(), resourceType: String = "image") = {
    val htmlOptions = if (paramHtmlOptions == null) Map[String, Any]() else paramHtmlOptions
    val htmlOptionsString = htmlOptions
      .filterNot(p => p._2 == null || p._1 == "class")
      .mapValues(escapeHtml)
      .map(p => {
        p._1
      } + "=\"" + p._2 + "\"").mkString(" ")
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

  protected def uploadTagParams(paramOptions: Map[String, Any] = Map(), resourceType: String = "image", signed: Boolean = true) = {
    var options = paramOptions

    val callback = options.get("callback") match {
      case None => cloudinary.getStringConfig("callback")
      case c@_ => c
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
