package com.cloudinary

import scala.language.postfixOps
import org.scalatest._
import org.scalatest.Matchers
import java.net.URI

class CloudinarySpec extends FlatSpec with Matchers with OptionValues with Inside {
  lazy val cloudinary = {
    new Cloudinary("cloudinary://a:b@test123")
  }

  behavior of "Cloudinary"

  it should "use cloud_name from config" in {
    cloudinary.url().generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/test")
  }

  it should "allow overriding cloud_name in options" in {
    cloudinary.url().cloudName("test321").generate("test") should equal(
      "http://res.cloudinary.com/test321/image/upload/test")
  }

  it should "use default secure distribution if secure=true" in {
    cloudinary.url().secure(true).generate("test") should equal(
      "https://res.cloudinary.com/test123/image/upload/test")
  }

  it should "allow overwriting secure distribution if secure=true" in {
    cloudinary.url()
      .secure(true)
      .secureDistribution("something.else.com").generate("test") should equal(
        "https://something.else.com/test123/image/upload/test")
  }

  it should "take secure distribution from config if secure=true" in {
    cloudinary.withConfig(Map("secure_distribution" -> "config.secure.distribution.com"))
      .url().secure(true).generate("test") should equal(
        "https://config.secure.distribution.com/test123/image/upload/test")
  }

  it should "default to akamai if secure is given with private_cdn and no secure_distribution" in {
    cloudinary.withConfig(Map(
      "secure" -> true,
      "private_cdn" -> true)).url().generate("test") should equal(
      "https://test123-res.cloudinary.com/image/upload/test")
  }

  it should "not add cloud_name if private_cdn and secure non akamai secure_distribution" in {
    cloudinary.withConfig(Map(
      "secure" -> true,
      "private_cdn" -> true,
      "secure_distribution" -> "something.cloudfront.net")).url().generate("test") should equal(
      "https://something.cloudfront.net/image/upload/test")
  }

  it should "not add cloud_name if private_cdn and not secure" in {
    cloudinary.withConfig(Map(
      "private_cdn" -> true)).url().generate("test") should equal(
      "http://test123-res.cloudinary.com/image/upload/test")

  }

  it should "should use format from options" in {
    cloudinary.url().format("jpg").generate("test") should equal(
      "http://res.cloudinary.com/test123/image/upload/test.jpg")
  }

  it should "should use type from options" in {
    cloudinary.url().`type`("facebook").generate("test") should equal(
      "http://res.cloudinary.com/test123/image/facebook/test")
  }

  it should "should use resource_type from options" in {
    cloudinary.url().resourceType("raw").generate("test") should equal(
      "http://res.cloudinary.com/test123/raw/upload/test")
  }

  it should "ignore http links only if type is not given or is asset" in {
    cloudinary.url().generate("http://test") should equal(
      "http://test")
    cloudinary.url().`type`("asset").generate("http://test") should equal(
      "http://test")
    cloudinary.url().`type`("fetch").generate("http://test") should equal(
      "http://res.cloudinary.com/test123/image/fetch/http://test")
  }

  it should "escape fetch urls" in {
    cloudinary.url().`type`("fetch").generate("http://blah.com/hello?a=b") should equal(
      "http://res.cloudinary.com/test123/image/fetch/http://blah.com/hello%3Fa%3Db")
  }

  it should "support external cname" in {
    cloudinary.url().cname("hello.com").generate("test") should equal(
      "http://hello.com/test123/image/upload/test")
  }

  it should "support external cname with cdn_subdomain on" in {
    cloudinary.url().cname("hello.com").cdnSubdomain(true).generate("test") should equal(
      "http://a2.hello.com/test123/image/upload/test")
  }

  it should "escape http urls" in {
    cloudinary.url().`type`("youtube").generate("http://www.youtube.com/watch?v=d9NF2edxy-M") should equal(
      "http://res.cloudinary.com/test123/image/youtube/http://www.youtube.com/watch%3Fv%3Dd9NF2edxy-M")
  }

  it should "support image tag generation with matching width and height" in {
    cloudinary.url()
      .transformation(Transformation().w_(100).h_(101).c_("crop"))
      .imageTag("test", Map("alt" -> "my image")) should equal(
        "<img alt=\"my image\" src=\"http://res.cloudinary.com/test123/image/upload/c_crop,h_101,w_100/test\" height=\"101\" width=\"100\" />")

    cloudinary.url()
      .transformation(Transformation().w_(0.9).h_(0.9).c_("crop").responsiveWidth(true))
      .imageTag("test", Map("alt" -> "my image")) should equal(
        "<img alt=\"my image\" class=\"cld-responsive\" data-src=\"http://res.cloudinary.com/test123/image/upload/c_crop,h_0.9,w_0.9/c_limit,w_auto/test\" />")

    cloudinary.url()
      .transformation(Transformation().w_(0.9).h_(0.9).c_("crop").responsiveWidth(true))
      .imageTag("test", Map("alt" -> "my image", "class" -> "extra")) should equal(
        "<img alt=\"my image\" class=\"extra cld-responsive\" data-src=\"http://res.cloudinary.com/test123/image/upload/c_crop,h_0.9,w_0.9/c_limit,w_auto/test\" />")

    cloudinary.url()
      .transformation(Transformation().width("auto").crop("crop"))
      .imageTag("test", Map("alt" -> "my image", "responsive_placeholder" -> "blank")) should equal(
        "<img alt=\"my image\" class=\"cld-responsive\" data-src=\"http://res.cloudinary.com/test123/image/upload/c_crop,w_auto/test\" src=\"data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7\" />")
    
    cloudinary.url()
      .transformation(Transformation().width("auto").crop("crop"))
      .imageTag("test", Map("alt" -> "my image", "responsive_placeholder" -> "other.gif")) should equal(
        "<img alt=\"my image\" class=\"cld-responsive\" data-src=\"http://res.cloudinary.com/test123/image/upload/c_crop,w_auto/test\" src=\"other.gif\" />")

  }

  it should "add version if public_id contains" in {
    cloudinary.url().generate("folder/test") should equal(
      "http://res.cloudinary.com/test123/image/upload/v1/folder/test")
    cloudinary.url().version(123).generate("folder/test") should equal(
      "http://res.cloudinary.com/test123/image/upload/v123/folder/test")
  }

  it should "not add version if public_id contains version already" in {
    cloudinary.url().generate("v123/test") should equal(
      "http://res.cloudinary.com/test123/image/upload/v123/test")
  }

  it should "allow to shorten image/upload urls" in {
    cloudinary.url().shorten(true).generate("test") should equal(
      "http://res.cloudinary.com/test123/iu/test")
  }

  it should "support private downloads" in {
    val request = cloudinary.privateDownload("img", "jpg") 
    val parameters = request.getParams()
    parameters.getFirstValue("public_id") should equal("img")
    parameters.getFirstValue("format") should equal("jpg")
    parameters.getFirstValue("api_key") should equal("a")
    request.getURI().getPath() should equal("/v1_1/test123/image/download")
  }

  it should "support zip downloads" in {
    val request = cloudinary.zipDownload("ttag")
    val parameters = request.getParams()
    parameters.getFirstValue("tag") should equal("ttag")
    parameters.getFirstValue("api_key") should equal("a")
    request.getURI().getPath() should equal("/v1_1/test123/image/download_tag.zip")
  }

  it should "support css sprite generation" in {
    cloudinary.url().generateSpriteCss("test") should equal(
      "http://res.cloudinary.com/test123/image/sprite/test.css")
    cloudinary.url().generateSpriteCss("test.css") should equal(
      "http://res.cloudinary.com/test123/image/sprite/test.css")
  }

  it should "correctly sign a url" in {
    cloudinary.url().version(1234).
      transformation(new Transformation().crop("crop").width(10).height(20)).
      signed(true).
      generate("image.jpg") should equal("http://res.cloudinary.com/test123/image/upload/s--MaRXzoEC--/c_crop,h_20,w_10/v1234/image.jpg")

    cloudinary.url().version(1234).
      signed(true).
      generate("image.jpg") should equal("http://res.cloudinary.com/test123/image/upload/s--ZlgFLQcO--/v1234/image.jpg");

    cloudinary.url().
      transformation(new Transformation().crop("crop").width(10).height(20)).
      signed(true).
      generate("image.jpg") should equal("http://res.cloudinary.com/test123/image/upload/s--Ai4Znfl3--/c_crop,h_20,w_10/image.jpg")
  }

  it should "support escape public ids" in {
    Map(
      "a b" -> "a%20b",
      "a+b" -> "a%2Bb",
      "a%20b" -> "a%20b",
      "a-b" -> "a-b",
      "a??b" -> "a%3F%3Fb").foreach{
      p =>
      val (pId, expected) = p
      cloudinary.url().generate(pId) should equal(
          "http://res.cloudinary.com/test123/image/upload/" + expected
       )       	  
    }
    cloudinary.url().generateSpriteCss("test") should equal(
      "http://res.cloudinary.com/test123/image/sprite/test.css")
    cloudinary.url().generateSpriteCss("test.css") should equal(
      "http://res.cloudinary.com/test123/image/sprite/test.css")
  }

}