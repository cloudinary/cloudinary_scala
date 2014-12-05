package com.cloudinary;

import java.security.SecureRandom
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.net.URI
import java.net.URLDecoder
import java.io.UnsupportedEncodingException
import _root_.com.ning.http.client.RequestBuilder

object Cloudinary {
  final val CF_SHARED_CDN = "d3jpl91pxevbkh.cloudfront.net";
  final val OLD_AKAMAI_SHARED_CDN = "cloudinary-a.akamaihd.net";
  final val AKAMAI_SHARED_CDN = "res.cloudinary.com";
  final val SHARED_CDN = AKAMAI_SHARED_CDN;
  final val VERSION = "0.9.4-SNAPSHOT"
  final val USER_AGENT = s"cld-scala-$VERSION"

  def configFromUrl(cloudinaryUrl: String): Map[String, Any] = {
    val cloudinaryUri = new URI(cloudinaryUrl);
    val creds = cloudinaryUri.getUserInfo().split(":")
    val m = Map(
      "cloud_name" -> cloudinaryUri.getHost,
      "api_key" -> creds(0),
      "api_secret" -> creds(1),
      "private_cdn" -> cloudinaryUri.getPath(),
      "secure_distribution" -> cloudinaryUri.getPath())

    if (cloudinaryUri.getQuery() != null) {
      m ++ cloudinaryUri.getQuery().split("&").map {
        param: String =>
          val kv = param.split("=")
          (kv.head -> URLDecoder.decode(kv.last, "ASCII"))
      }
    } else m
  }

  private def bytes2Hex(bytes: Array[Byte]): String = bytes.map("%02X" format _).mkString

  private final val RND = new SecureRandom();

  def signedPreloadedImage(result: Map[String, _]): String =
    (for {
      resourceType <- result.get("resource_type")
      version <- result.get("version")
      publicId <- result.get("public_id")
      signature <- result.get("signature")
    } yield {
      val fmt = result.get("format").map("." + _).getOrElse("")
      s"$resourceType/upload/v$version/$publicId$fmt#$signature"
    }).get

  def apiSignRequest(paramsToSign: Map[String, _], apiSecret: String) = {

    val params = paramsToSign.toList.sortBy(_._1) collect {
      case (k, v: Iterable[_]) => s"$k=${v.mkString(",")}"
      case (k, v) if (v != null && v.toString() != "") => s"$k=$v"
    }

    val digest = sign(params.mkString("&"), apiSecret)
    
    bytes2Hex(digest).toLowerCase()
  }
  
  def sign(toSign:String, apiSecret: String) = {
    var md: MessageDigest = null
    try {
      md = MessageDigest.getInstance("SHA-1")
    } catch {
      case e: NoSuchAlgorithmException => throw new RuntimeException("Unexpected exception", e);
    }

    md.digest((toSign + apiSecret).getBytes());
  }

  private[cloudinary] def randomPublicId() = {
    val bytes = new Array[Byte](8);
    RND.nextBytes(bytes);
    bytes2Hex(bytes);
  }

  private[cloudinary] def asString(value: Any, defaultValue: Option[String] = None): Option[String] =
    value match {
      case null => defaultValue
      case "" => defaultValue
      case x: Option[_] => x match {
        case Some(x) => asString(x, defaultValue)
        case None => defaultValue
      }
      case _ => Some(value.toString())
    }

  private[cloudinary] def asBoolean(value: Option[_]): Option[Boolean] = {
    value collect {
      case v: Boolean => v
      case x: String => "true" == x
    }
  }

  private[cloudinary] def asBoolean(value: Any, defaultValue: Boolean): Boolean = {
    value match {
      case null => defaultValue
      case x: Option[_] => x match {
        case Some(x) => asBoolean(x, defaultValue)
        case None => defaultValue
      }
      case v: Boolean => v
      case x @ _ => "true" == value
    }
  }

  private[cloudinary] def cleanupEmpty(params: Map[String, Any]) = params.filterNot(p => Cloudinary.emptyValue(p._2))

  private def emptyValue(v: Any): Boolean = v match {
    case null => true
    case o: Option[_] if o.isEmpty => true
    case _ if "".equals(v) => true
    case _ => false
  }
}

class Cloudinary(config: Map[String, Any]) {

  def this(cloudinaryUrl: String) {
    this(Cloudinary.configFromUrl(cloudinaryUrl));
  }

  def this() {
    this(Cloudinary.configFromUrl(
      System.getProperty("CLOUDINARY_URL", System.getenv("CLOUDINARY_URL"))))
  }

  def withConfig(overridingConfig: Map[String, Any]) = {
    new Cloudinary(config ++ overridingConfig)
  }

  implicit val self = this

  def url() = new Url(this)

  def uploader() = new Uploader()

  def api() = new Api()

  def cloudinaryApiUrlPrefix(): String = {
    val cloudinary = Cloudinary.asString(config.get("upload_prefix"),
      Some("https://api.cloudinary.com")).get

    val cloudName = Cloudinary.asString(config.get("cloud_name")).get

    if (cloudName.isEmpty) throw new IllegalArgumentException("Must supply cloud_name in tag or in configuration")
    s"$cloudinary/v1_1/$cloudName"
  }

  def cloudinaryApiUrl(action: String, resourceType: String): String = {
    val prefix = cloudinaryApiUrlPrefix()
    s"$prefix/$resourceType/$action"
  }

  def apiKey(options: Map[String, Any] = Map()) = {
    val result = Cloudinary.asString(options.get("api_key"), this.getStringConfig("api_key"))
    if (result.isEmpty)
      throw new IllegalArgumentException("Must supply api_key")
    result.get
  }

  def apiSecret(options: Map[String, Any] = Map()) = {
    val result = Cloudinary.asString(options.get("api_secret"), this.getStringConfig("api_secret"))
    if (result.isEmpty)
      throw new IllegalArgumentException("Must supply api_secret")
    result.get
  }

  def signRequest(params: Map[String, Any]): Map[String, Any] = {
    val key = apiKey()
    val secret = apiSecret()
    val filteredParams = Cloudinary.cleanupEmpty(params)
    filteredParams +
      ("signature" -> Cloudinary.apiSignRequest(filteredParams, secret)) +
      ("api_key" -> key)
  }

  def privateDownload(publicId: String, format: String, resourceType: String = "image", attachment: Option[String] = None, `type`: Option[String] = None) = {
    var params = Map[String, Any](
      "public_id" -> publicId,
      "format" -> format,
      "attachment" -> attachment,
      "type" -> `type`,
      "timestamp" -> (System.currentTimeMillis() / 1000L).toLong.toString())
    params = signRequest(params);
    val builder = new RequestBuilder("GET")
      .setUrl(cloudinaryApiUrl("download", resourceType))

    for (param <- params) {
      builder.addParameter(param._1, param._2.toString())
    }
    builder.build()
  }

  def privateDownloadUrl(publicId: String, format: String, resourceType: String = "image", attachment: Option[String] = None, `type`: Option[String] = None) = privateDownload(publicId, format, resourceType, attachment, `type`).getUrl()

  def zipDownload(tag: String, resourceType: String = "image", transformation: Option[Transformation] = None) = {
    var params = Map[String, Any](
      "timestamp" -> (System.currentTimeMillis() / 1000L).toLong.toString(),
      "tag" -> tag)

    transformation match {
      case Some(transformation: Transformation) => params = params +
        ("transformation" -> transformation.generate())
      case _ =>
    }

    params = signRequest(params)
    val builder = new RequestBuilder("GET")
      .setUrl(cloudinaryApiUrl("download_tag.zip", resourceType))

    for (param <- params) {
      builder.addParameter(param._1, param._2.toString())
    }
    builder.build()
  }

  def zipDownloadUrl(tag: String, resourceType: String = "image") = zipDownload(tag, resourceType).getUrl()

  def getBooleanConfig(key: String, defaultValue: Boolean): Boolean =
    Cloudinary.asBoolean(config.getOrElse(key, null), defaultValue)

  def getOptionalBooleanConfig(key: String): Option[Boolean] =
    Cloudinary.asBoolean(config.get(key))

  def getStringConfig(key: String, defaultValue: Option[String] = None): Option[String] =
    Cloudinary.asString(config.getOrElse(key, null), defaultValue)

}
