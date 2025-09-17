package com.cloudinary

import java.net.URLDecoder
import com.ning.http.util.Base64

case class Url(
  cloudName: String,
  secure: Boolean = false,
  privateCdn: Boolean = false,
  secureDistribution: Option[String] = None,
  cdnSubdomain: Boolean = false,
  secureCdnSubdomain: Option[Boolean] = None,
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
  defaultDPR: Option[String] = None,
  urlSuffix: Option[String] = None,
  useRootPath: Boolean = false) {
  def this(cloudinary: Cloudinary) {
    this(
      cloudName = cloudinary.getStringConfig("cloud_name").get,
      secureDistribution = cloudinary.getStringConfig("secure_distribution"),
      cname = cloudinary.getStringConfig("cname"),
      secure = cloudinary.getBooleanConfig("secure", false),
      privateCdn = cloudinary.getBooleanConfig("private_cdn", false),
      cdnSubdomain = cloudinary.getBooleanConfig("cdn_subdomain", false),
      secureCdnSubdomain = cloudinary.getOptionalBooleanConfig("secure_cdn_subdomain"),
      shorten = cloudinary.getBooleanConfig("shorten", false),
      signUrl = cloudinary.getBooleanConfig("sign_url", false),
      apiSecret = cloudinary.getStringConfig("api_secret"),
      defaultResponsive = cloudinary.getBooleanConfig("responsive_width", false),
      responsiveTransformation = cloudinary.getStringConfig("responsive_transformation"),
      responsivePlaceholder = cloudinary.getStringConfig("responsive_placeholder"),
      defaultDPR = cloudinary.getStringConfig("dpr"),
      useRootPath = cloudinary.getBooleanConfig("use_root_path", false)
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
  def secureCdnSubdomain(secureCdnSubdomainValue: Boolean): Url = copy(secureCdnSubdomain = Some(secureCdnSubdomainValue))
  def shorten(shortenValue: Boolean): Url = copy(shorten = shortenValue)
  def signed(signUrlValue: Boolean): Url = copy(signUrl = signUrlValue)
  def urlSuffix(urlSuffixValue: String): Url = copy(urlSuffix = Some(urlSuffixValue))
  def useRootPath(useRootPathValue: Boolean): Url = copy(useRootPath = useRootPathValue)

  /**
   * cdn_subdomain and secure_cdn_subdomain
   * 1) Customers in shared distribution (e.g. res.cloudinary.com)
   *   if cdn_domain is true uses res-[1-5].cloudinary.com for both http and https. Setting secure_cdn_subdomain to false disables this for https.
   * 2) Customers with private cdn
   *   if cdn_domain is true uses cloudname-res-[1-5].cloudinary.com for http
   *   if secure_cdn_domain is true uses cloudname-res-[1-5].cloudinary.com for https (please contact support if you require this)
   * 3) Customers with cname
   *   if cdn_domain is true uses a[1-5].cname for http. For https, uses the same naming scheme as 1 for shared distribution and as 2 for private distribution.
   */
  private def getPrefix(source: String): String = {
    if (secure)
      getSecurePrefix(source)
    else {
      val prefix = cname.map{ cname =>
        val subdomain = if (cdnSubdomain) s"a${shard(source)}." else ""
        s"http://$subdomain$cname"
      }.getOrElse {
        val host = List(
          if (privateCdn) s"$cloudName-" else "",
          "res",
          if (cdnSubdomain) s"-${shard(source)}" else "",
          ".cloudinary.com").mkString
        s"http://$host"
      }
      if (!privateCdn) s"$prefix/$cloudName" else prefix
    }
  }

  private def getSecurePrefix(source:String): String = {
    val secureDistribution =
      this.secureDistribution match {
        case None | Some(Cloudinary.OLD_AKAMAI_SHARED_CDN) if (privateCdn) => s"$cloudName-res.cloudinary.com"
        case None | Some(Cloudinary.OLD_AKAMAI_SHARED_CDN) => Cloudinary.SHARED_CDN
        case Some(value) => value
      }

    val sharedDomain = !privateCdn || secureDistribution == Cloudinary.SHARED_CDN

    val secureCdnSubdomain = this.secureCdnSubdomain.getOrElse(
      if (sharedDomain) cdnSubdomain else false
    )

    val distribution = if (secureCdnSubdomain)
      secureDistribution.replaceAll("res.cloudinary.com", s"res-${shard(source)}.cloudinary.com")
    else secureDistribution

    val prefix = s"https://$distribution"

    if (sharedDomain) prefix + s"/$cloudName" else prefix
  }

  private def shard(source:String) = {
    val crc32 = new java.util.zip.CRC32()
    crc32.update(source.getBytes())
    ((crc32.getValue() % 5 + 5) % 5 + 1).toString
  }


  def generate(source: String):String = {
    if (!privateCdn && urlSuffix.isDefined) throw new IllegalArgumentException("URL Suffix only supported in private CDN")
    if (!privateCdn && useRootPath) throw new IllegalArgumentException("Root path only supported in private CDN")
    if (cloudName == null || cloudName == "") throw new IllegalArgumentException("Must supply cloud_name in tag or in configuration")
    if (`type` == "upload" && source.toLowerCase.matches("^https?:/.*")) return source

    val (trns, format) = (`type`, this.format) match {
      case ("fetch", Some(format)) => (transformation.orElse(Some( new Transformation )).map(_.fetchFormat(format)), None)
      case _ => (transformation, this.format)
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

    val version = (if (source.contains("/") &&
                      !source.matches("v[0-9]+.*") &&
                      !source.matches("https?:/.*") &&
                      this.version.isEmpty) Some("1") else this.version).map("v" + _)

    val (finalSource, signableSource) = finalizeSource(source)

    val signature = if (signUrl) {
      val toSign = List(transformationStr, Some(signableSource)).flatten.mkString("/")
      Some("s--" +
          Base64.encode(Cloudinary.sign(toSign, apiSecret.getOrElse(throw new Exception("Must supply api secret to sign URLs")))).
          	take(8).
          	replace('+', '-').replace('/', '_') + "--")
    } else None

    val pathComps = List(Some(prefix)) ++ finalizedResourceType ++ List(signature, transformationStr, version, Some(finalSource))
    pathComps.flatten.mkString("/").replaceAll("([^:])\\/+", "$1/")
  }

  private def finalizeSource(inSource:String):(String, String) = {
    val source = inSource.replaceAll("([^:])\\/+", "$1/")
    if (source.toLowerCase().matches("^https?:/.*")) {
      val encodedSource = SmartUrlEncoder.encode(source)
      (encodedSource, encodedSource)
    } else {
      val encodedSource = SmartUrlEncoder.encode(URLDecoder.decode(source.replace("+", "%2B"), "UTF-8"))
      val formatSuffix = format.map("." + _).getOrElse("")
      urlSuffix match {
        case Some(suffix) if suffix.matches(".*[\\./].*") => throw new IllegalArgumentException("urlSuffix should not include . or /")
        case Some(suffix) => (s"$encodedSource/$suffix$formatSuffix", encodedSource + formatSuffix)
        case _ => (encodedSource + formatSuffix, encodedSource + formatSuffix)
      }
    }
  }

  private val finalizedResourceType =
    ((resourceType, `type`, urlSuffix, useRootPath, shorten) match {
      case ("image", "upload", _, true, _) => List()
      case (_, _, _, true, _) => throw new IllegalArgumentException("Root path only supported for image/upload")
      case ("image", "upload", Some(urlSuffix), _, _) => List("images")
      case ("raw", "upload", Some(urlSuffix), _, _) => List("files")
      case (_, _, Some(urlSuffix), _, _) => throw new IllegalArgumentException("URL Suffix only supported for image/upload and raw/upload")
      case ("image", "upload", _, _, true) => List("iu")
      case (rt, t, _, _, _) => List(rt, t)
    }).map{Some(_)}


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
