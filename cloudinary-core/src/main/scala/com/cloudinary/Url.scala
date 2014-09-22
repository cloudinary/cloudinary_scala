package com.cloudinary

import java.net.URLDecoder
import com.ning.http.util.Base64

case class Url(
  cloudName: String,
  secure: Boolean = false,
  privateCdn: Boolean = false,
  secureDistribution: Option[String] = None,
  cdnSubdomain: Boolean = false,
  shorten: Boolean = false,
  cname: Option[String] = None,
  `type`: String = "upload",
  resourceType: String = "image",
  format: Option[String] = None,
  version: Option[String] = None,
  transformation: Option[Transformation] = None,
  signUrl: Boolean = false,
  apiSecret: Option[String] = None,
  defaultResponsive: Boolean = false,
  responsiveTransformation: Option[String] = None,
  responsivePlaceholder: Option[String] = None,
  defaultDPR: Option[String] = None) {
  def this(cloudinary: Cloudinary) {
    this(
      cloudName = cloudinary.getStringConfig("cloud_name").get,
      secureDistribution = cloudinary.getStringConfig("secure_distribution"),
      cname = cloudinary.getStringConfig("cname"),
      secure = cloudinary.getBooleanConfig("secure", false),
      privateCdn = cloudinary.getBooleanConfig("private_cdn", false),
      cdnSubdomain = cloudinary.getBooleanConfig("cdn_subdomain", false),
      shorten = cloudinary.getBooleanConfig("shorten", false),
      signUrl = cloudinary.getBooleanConfig("sign_url", false),
      apiSecret = cloudinary.getStringConfig("api_secret"),
      defaultResponsive = cloudinary.getBooleanConfig("responsive_width", false),
      responsiveTransformation = cloudinary.getStringConfig("responsive_transformation"),
      responsivePlaceholder = cloudinary.getStringConfig("responsive_placeholder"),
      defaultDPR = cloudinary.getStringConfig("dpr")
    )
  }

  def `type`(t: String): Url = copy(`type` = t)
  def resourceType(resourceTypeValue: String): Url = copy(resourceType = resourceTypeValue)
  def format(formatValue: String): Url = copy(format = Option(formatValue))
  def cloudName(cloudNameValue: String): Url = copy(cloudName = cloudNameValue)
  def secureDistribution(secureDistributionValue: String): Url = copy(secureDistribution = Option(secureDistributionValue))
  def cname(cnameValue: String): Url = copy(cname = Option(cnameValue))
  def version(version: Any): Url = copy(version = Cloudinary.asString(version))
  def transformation(transformationValue: Transformation): Url = copy(transformation = Option(transformationValue))
  def secure(secureValue: Boolean): Url = copy(secure = secureValue)
  def privateCdn(privateCdnValue: Boolean): Url = copy(privateCdn = privateCdnValue)
  def cdnSubdomain(cdnSubdomainValue: Boolean): Url = copy(cdnSubdomain = cdnSubdomainValue)
  def shorten(shortenValue: Boolean): Url = copy(shorten = shortenValue)
  def signed(signUrlValue: Boolean): Url = copy(signUrl = signUrlValue)

  private def getPrefix(source: String): String = {
    var result = ""
    var sharedDist = secureDistribution.getOrElse(Cloudinary.OLD_AKAMAI_SHARED_CDN)
    var sharedDomain = !privateCdn

    if (secure) {
      if (sharedDist.equals(Cloudinary.OLD_AKAMAI_SHARED_CDN)) {
        sharedDist = if (privateCdn)
          cloudName + "-res.cloudinary.com"
        else
          Cloudinary.SHARED_CDN;
      }
      sharedDomain = sharedDomain || Cloudinary.SHARED_CDN.equals(sharedDist)
      result = "https://" + sharedDist
    } else {
      val crc32 = new java.util.zip.CRC32()
      crc32.update(source.getBytes())
      val subdomain = if (cdnSubdomain) "a" + ((crc32.getValue() % 5 + 5) % 5 + 1) + "." else ""
      val host = cname match {
        case Some(h) => h
        case _ => (if (privateCdn) cloudName + "-" else "") + "res.cloudinary.com"
      }
      result = "http://" + subdomain + host;
    }
    if (sharedDomain) result = result + "/" + cloudName
    result
  }

  private def generateFromEscaped(source: String) = {
    var trns = transformation
    var format = this.format
    var resourceType = this.resourceType
    var `type` = this.`type`
    var version = this.version

    if (`type`.equals("fetch") && format.isDefined) {
      trns = if (trns.isEmpty) Some(new Transformation()) else trns
      trns = trns.map(_.fetchFormat(format.get))
      format = None
    }

    val transformationStr = trns.map{ ot =>
      var t = defaultDPR.map{dpr => ot.dpr(dpr)}.getOrElse(ot)
      if (defaultResponsive) t = t.responsiveWidth(true)
      val str = t.generate
      if (t.isResponsive && !t.hasWidthAuto) {
        str + "/"+responsiveTransformation.getOrElse("c_limit,w_auto")
      } else {
        str
      }
    }
    val prefix = getPrefix(source)

    if (shorten && resourceType.equals("image") && `type`.equals("upload")) {
      resourceType = "iu";
      `type` = "";
    }

    if (source.contains("/") && !source.matches("v[0-9]+.*") && !source.matches("https?:/.*") && version.isEmpty) {
      version = Some("1");
    }

    version = version.map("v" + _)

    val rest = List(transformationStr, version, Some(source)).flatten.mkString("/")
    val signature = if (signUrl) {
      Some("s--" + 
          Base64.encode(Cloudinary.sign(rest, apiSecret.getOrElse(throw new Exception("Must supply api secret to sign URLs")))).
          	take(8).
          	replace('+', '-').replace('/', '_') + "--")
    } else None
    
    val pathComps: List[Option[String]] = List(Some(prefix), Some(resourceType), Some(`type`), signature, Some(rest))
    pathComps.flatten.mkString("/").replaceAll("([^:])\\/+", "$1/")
  }

  def generate(source: String):String = {
    val remote = source.toLowerCase().matches("^https?:/.*")
    source match {
      case _ if (cloudName == null || cloudName == "") => throw new IllegalArgumentException("Must supply cloud_name in tag or in configuration")
      case null => throw new IllegalArgumentException("Must supply cloud_name in tag or in configuration")
      case _ if remote && ("upload" == `type` || "asset" == `type`) => source
      case _ if remote  => generateFromEscaped(SmartUrlEncoder.encode(source))
      case _ =>
        val escaped = SmartUrlEncoder.encode(URLDecoder.decode(source.replace("+", "%2B"), "UTF-8"))
        val escapedFormatted = format.map(f => escaped + "." + f).getOrElse(escaped)
        generateFromEscaped(escapedFormatted)
    }

  }

  def generateSpriteCss(source: String) = {
    copy(`type` = "sprite")
      .copy(format = if (!source.endsWith(".css")) Some("css") else format)
      .generate(source)
  }

  def imageTag(source: String, attributes: Map[String, String] = Map()) = {
    var url = generate(source)
    val isResponsive = transformation.map(_.isResponsive).getOrElse(false)
    val isHiDPI = transformation.map(_.isHiDPI).getOrElse(false)
    val placeHolder = attributes.get("responsive_placeholder").orElse(responsivePlaceholder) match {
      case Some("blank") => Some("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")
      case Some(p) => Some(p)
      case _ => None
    }
    val attributesHtml = ((attributes - "class" - "responsive_placeholder") ++ (attributes.get("class") match {
      case Some(cls) if isResponsive => List(("class", s"$cls cld-responsive"))
      case Some(cls) if isHiDPI => List(("class", s"$cls cld-hidpi"))
      case None if isResponsive => List(("class", "cld-responsive"))
      case None if isHiDPI => List(("class", "cld-hidpi"))
      case Some(cls) => List(("class", cls))
      case _ => List()
    }) ++ (placeHolder match {
      case Some(ph) if isResponsive || isHiDPI => List(("src", ph),("data-src", url))
      case None if isResponsive || isHiDPI => List(("data-src", url))
      case _ => List(("src", url))
    })).collect{case (attr, value) => s"""$attr="$value""""}.toList.sorted.mkString(" ") +
        transformation.flatMap(_.htmlHeight.map(h => s""" height="$h"""")).getOrElse("") +
        transformation.flatMap(_.htmlWidth.map(w => s""" width="$w"""")).getOrElse("")
    s"""<img $attributesHtml />"""
  }

}
