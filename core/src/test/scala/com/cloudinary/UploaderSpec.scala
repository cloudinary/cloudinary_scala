package com.cloudinary

import java.util.concurrent.TimeoutException

import com.cloudinary.Implicits._
import com.cloudinary.parameters._
import com.cloudinary.response._
import com.ning.http.client.Request
import com.ning.http.client.multipart.StringPart
import org.scalatest.{Matchers, _}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object UploadPresetTest extends Tag("com.cloudinary.tags.UploadPresetTest")
object EagerTest extends Tag("com.cloudinary.tags.EagerTest")

class UploaderSpec extends MockableFlatSpec with Matchers with OptionValues with Inside with BeforeAndAfterAll {

  private val testResourcePath = "cloudinary-core/src/test/resources"
  private val uploadTag = testTag + "_upload"
  private val options = UploadParameters().tags(Set(prefix, testTag, uploadTag))
  private val uploader : Uploader = cloudinary.uploader()

  private val api = cloudinary.api()

  override def afterAll(): Unit = {
    super.afterAll()
    api.deleteResourcesByTag(uploadTag)
  }

  behavior of "An Uploader"

  it should "upload a file" in {
    val publicId: String = "aåßéƒ"
    val result = Await result (
      uploader
      .upload(s"$testResourcePath/logo.png", options.colors(true).publicId(publicId)), 5 seconds)

    result.public_id should equal(publicId)
    result.width should equal(241)
    result.height should equal(51)
    result.colors should not equal (Map())
    result.predominant should not equal (Map())
    result.pages should equal(1)

    val toSign = Map(
      "public_id" -> result.public_id,
      "version" -> result.version)
    val expectedSignature = Cloudinary.apiSignRequest(toSign, cloudinary.apiSecret()).toLowerCase()
    result.signature should equal(expectedSignature)
  }

  it should "upload a file and get sizes, colors, predominant, public_id, version and signature back" in {
    val result = Await result (
      uploader
      .upload(s"$testResourcePath/logo.png", options.colors(true)), 5 seconds)

    result.width should equal(241)
    result.height should equal(51)
    result.colors should not equal (Map())
    result.predominant should not equal (Map())
    result.pages should equal(1)

    val toSign = Map(
      "public_id" -> result.public_id,
      "version" -> result.version)
    val expectedSignature = Cloudinary.apiSignRequest(toSign, cloudinary.apiSecret()).toLowerCase()
    result.signature should equal(expectedSignature)
  }

  it should "upload from a url" in {
    val result = Await result (
      uploader.upload("http://cloudinary.com/images/logo.png"), 5 seconds)
    result.width should equal(344)
    result.height should equal(76)

    val toSign = Map(
      "public_id" -> result.public_id,
      "version" -> result.version)
    val expectedSignature = Cloudinary.apiSignRequest(toSign, cloudinary.apiSecret()).toLowerCase()
    result.signature should equal(expectedSignature)
  }

  it should "return face coordinates when requested" in {
    val result = Await result (
      uploader.upload("http://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Bill_Clinton.jpg/220px-Bill_Clinton.jpg",
        options.faces(true)), 5 seconds)
    result.faces.size should equal(1)
  }

  it should "upload from data" in {
    val result = Await result (
      uploader.upload("data:image/png;base64,iVBORw0KGgoAA\nAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAABlBMVEUAAAD///+l2Z/dAAAAM0l\nEQVR4nGP4/5/h/1+G/58ZDrAz3D/McH8yw83NDDeNGe4Ug9C9zwz3gVLMDA/A6\nP9/AFGGFyjOXZtQAAAAAElFTkSuQmCC"), 5 seconds)

    result.width should equal(16)
    result.height should equal(16)

    val toSign = Map(
      "public_id" -> result.public_id,
      "version" -> result.version)
    val expectedSignature = Cloudinary.apiSignRequest(toSign, cloudinary.apiSecret()).toLowerCase()
    result.signature should equal(expectedSignature)
  }

  it should "be able to rename files, fail if exist or override if override is specified" in {
    val result = Await result (
      uploader.upload(s"$testResourcePath/logo.png"), 5 seconds)

    val publicId1 = result.public_id

    Await result (
      uploader.rename(publicId1, publicId1 + "2") andThen {
        case r => api.resource(publicId1 + "2")
      }, 5 seconds) should not eq null

    val result2 = Await result (
      uploader.upload(s"$testResourcePath/favicon.ico"), 5 seconds)
    val publicId2 = result2.public_id

    val result3 = Await.result(
      uploader.rename(publicId2, publicId1 + "2")
        .map(r => new Exception("")).recover {
          case t => t
        }, 5 seconds)

    result3.getClass() should equal(classOf[BadRequest])

    val result4 = Await result (
      uploader.rename(publicId2, publicId1 + "2", overwrite = true) andThen {
        case Success(json) =>
          api.resource(publicId1 + "2")
      }, 5 seconds)
    result4.format should equal("ico")
  }

  it should "handle explicit upload" in {
    val result = Await.result(
      uploader.explicit("cloudinary",
        eager = List(Transformation().c_("scale").w_(2.0)),
        `type` = "twitter_name"), 5 seconds)
    val Some(url) = result.eager.headOption.map(_.url)
    var expectedUrl = cloudinary.url().`type`("twitter_name")
      .transformation(new Transformation().crop("scale").width(2.0))
      .format("png").version(result.version).generate("cloudinary")
    if (!cloudinary.cloudinaryApiUrlPrefix().startsWith("https://api.cloudinary.com")) {
      expectedUrl = expectedUrl.replaceFirst("http://res\\.cloudinary\\.com", "/res")
    }
    expectedUrl should equal(url)
  }

  it should "attach headers when specified, both as maps and as lists" in {
    Await.result(
      uploader.upload(s"$testResourcePath/logo.png", options.headers(Map("Link" -> "1"))), 5 seconds)
  }

  it should "support adding text" in {
    val result = Await.result(
      uploader.text(TextParameters("hello world")), 5 seconds)
    result.width.get should be > (1)
    result.height.get should be > (1)
  }

  it should "support generating sprites" in {
    val sprite_test_tag: String = "sprite_test_tag" + suffix
    val (provider, uploader )= mockUploader()
    val tagPart: StringPart = new StringPart("tag", sprite_test_tag, "UTF-8")
    (provider.execute _) expects where { (request: Request, *) => {
      val map = getParts(request)
      map.contains(("tag", sprite_test_tag))
    }
    }
    (provider.execute _) expects where { (request: Request, *) => {
      val map = getParts(request)
      map.contains(("tag", sprite_test_tag)) &&
        map.contains(("transformation", "w_100"))
    }
    }
    (provider.execute _) expects where { (request: Request, *) => {
      val map = getParts(request)
      map.contains(("tag", sprite_test_tag)) &&
        map.contains(("transformation", "f_jpg,w_100"))
    }
    }
    uploader.generateSprite(sprite_test_tag)
    uploader.generateSprite(sprite_test_tag, transformation = new Transformation().w_(100))
    uploader.generateSprite(sprite_test_tag, transformation = new Transformation().w_(100), format = "jpg")
  }

  it should "support multi" in {
    val (url1, url2, url3) = Await.result(for {
      r1 <- uploader.upload("http://cloudinary.com/images/logo.png",
        options.tags(Set("multi_test_tag")).publicId("multi_test_tag_1"))
      r2 <- uploader.upload("http://cloudinary.com/images/logo.png",
        options.tags(Set("multi_test_tag")).publicId("multi_test_tag_2"))
      url1 <- uploader.multi("multi_test_tag").map(_.url) if (r1 != null && r2 != null)
      url2 <- uploader.multi("multi_test_tag", transformation = Transformation().w_(100)).map(_.url) if (r1 != null && r2 != null)
      url3 <- uploader.multi("multi_test_tag", transformation = Transformation().w_(101), format = "pdf").map(_.url) if (r1 != null && r2 != null)
    } yield (url1, url2, url3), 20 seconds)

    url1 should endWith(".gif")
    url2 should include("w_100")
    url3 should include("w_101")
    url3 should endWith(".pdf")
  }

  it should "support tag operations on resources" in {
    Await.result(for {
      id1 <- uploader.upload(s"$testResourcePath/logo.png").map(_.public_id)
      id2 <- uploader.upload(s"$testResourcePath/logo.png").map(_.public_id)
      tagResult1 <- uploader.addTag("tag1", List(id1, id2))
      tagResult2 <- uploader.addTag("tag2", List(id1))
      tags1 <- api.resource(id1).map(_.tags)
      tags2 <- api.resource(id2).map(_.tags)
      tagResult3 <- uploader.removeTag("tag1", List(id1))
      tags3 <- api.resource(id1).map(_.tags)
      tagResult4 <- uploader.replaceTag("tag3", List(id1))
      tags4 <- api.resource(id1).map(_.tags)
    } yield (tags1, tags2, tags3, tags4), 20 seconds) should equal(
      (List("tag1", "tag2"), List("tag1"), List("tag2"), List("tag3")))
  }

  it should "allow whitelisted formats if allowed_formats" in {
    Await.result(for {
      result <- uploader.upload(s"$testResourcePath/logo.png", options.allowedFormats(Set("png")))
    } yield result.format, 5.seconds) should equal("png")
  }

  it should "prevent non whitelisted formats from being uploaded if allowed_formats is specified" in {
    Await.result(for {
      error <- uploader.upload(s"$testResourcePath/logo.png", options.allowedFormats(Set("jpg"))).recover{case e => e}
    } yield {error}, 5.seconds).isInstanceOf[BadRequest] should equal(true)  
  }

  it should "allow non whitelisted formats if type is specified and convert to that type" in {
    Await.result(for {
      result <- uploader.upload(s"$testResourcePath/logo.png", options.allowedFormats(Set("jpg")).format("jpg"))
    } yield result.format, 5.seconds) should equal("jpg")
  }

  it should "allow sending face coordinates" in {
    val faces1 = List(FaceInfo(121, 31, 110, 51), FaceInfo(120, 30, 109, 51))
    val faces2 = List(FaceInfo(122, 32, 111, 152))
    Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", options.faceCoordinates (faces1))
      resource1 <- api.resource(r1.public_id, faces=true) if r1 != null
      r2 <- uploader.explicit(r1.public_id, `type` = "upload", faceCoordinates = faces2) if resource1 != null
      resource2 <- api.resource(r1.public_id, faces=true) if r2 != null
    } yield {
      resource1.faces should equal(faces1)
      resource2.faces should equal(faces2)
    }, 10.seconds)
  }

  it should "create an upload tag" in {
    val tag = uploader.imageUploadTag("test-field", UploadParameters().callback("http://localhost/cloudinary_cors.html"), Map("htmlattr" -> "htmlvalue"))
    tag should include("type=\"file\"")
    tag should include("data-cloudinary-field=\"test-field\"")
    tag should include("class=\"cloudinary-fileupload\"")
    tag should include("htmlattr=\"htmlvalue\"")
    tag should include("type=\"file\"")
    uploader.imageUploadTag("test-field",
      UploadParameters().callback("http://localhost/cloudinary_cors.html"),
      Map("class" -> "myclass")) should include("class=\"cloudinary-fileupload myclass\"")
  }
  
  it should "support requesting manual moderation" in {
    Await.result(for  {
      result <- uploader.upload(s"$testResourcePath/logo.png", options.moderation("manual"))
    } yield {
      result.moderation.head.status should equal(com.cloudinary.response.ModerationStatus.pending)
      result.moderation.head.kind should equal("manual")
    }, 10.seconds)
  }
  
  it should "support requesting raw conversion" in {
    val error = Await.result(for {
      e <- uploader.upload(s"$testResourcePath/docx.docx", options.rawConvert("illegal"), "raw").recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("not a valid")
  }
  
  it should "support requesting categorization" in {
    val error = Await.result(for {
      e <- uploader.upload(s"$testResourcePath/logo.png", options.categorization("illegal")).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("is invalid")
  }
  
  it should "support requesting detection" in {
    val error = Await.result(for {
      e <- uploader.upload(s"$testResourcePath/logo.png", options.detection("illegal")).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("not a valid")
  }
    
  it should "support requesting auto_tagging" in {
    val error = Await.result(for {
      e <- uploader.upload(s"$testResourcePath/logo.png", options.autoTagging(0.5)).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("Must use")
  }
  
  it should "support uploading large raw files" in {
    Await.result(for {
      response <- uploader.uploadLargeRaw(s"$testResourcePath/docx.docx", LargeUploadParameters().tags(Set("large_upload_test_tag")))
    } yield {
      response.bytes should equal(new java.io.File(s"$testResourcePath/docx.docx").length())
      response.tags should equal(List("large_upload_test_tag"))
      response.done should equal(Some(true))
    }, 10.seconds)
  }

  it should "support unsigned uploading using presets" taggedAs(UploadPresetTest) in {
    val c = cloudinary.withConfig(Map("api_key" -> null, "api_secret" -> null)) 
    val (presetName, uploadResult) = Await.result(for {
      preset <- api.createUploadPreset(UploadPreset(unsigned = true, settings = options.folder("upload_folder")))
      result <- uploader.unsignedUpload(s"$testResourcePath/logo.png", preset.name)
    } yield (preset.name, result), 10.seconds)
    uploadResult.public_id should fullyMatch regex "upload_folder/[a-z0-9]+"
    Await.result(api.deleteUploadPreset(presetName), 5.seconds)
  }

  it should "support uploading with eager async transformations" taggedAs(EagerTest) in {
    val eagerTransforms = List(Transformation().c_("scale").w_(0.5), Transformation().c_("scale").w_(0.4))
    val uploadParams = options.eager(eagerTransforms).eagerAsync(true)
    Await.result(for {
      response <- uploader.upload("http://cloudinary.com/images/logo.png", uploadParams)
    } yield {
      response.eager.length should equal(2)
    }, 10.seconds)
  }

  it should "support a timeout argument" in {
    val uploadParams = UploadParameters()
    val  handler = ServerMock.fixedHandler(() => {
      Thread.sleep(3000)
      ServerMock.simpleResponse("ok")
    })
    new ServerMock(handler, () => {
      val startTime = System.currentTimeMillis()
      val mockCld = cloudinary.withConfig(Map("upload_prefix" -> s"http://localhost:${ServerMock.TEST_SERVER_PORT}"))
      val f = mockCld.uploader().upload("http://cloudinary.com/images/logo.png", uploadParams, requestTimeout = Some(1000))
      Await.ready(f, 10.seconds).value should matchPattern {
        case Some(Failure(t:TimeoutException)) =>
      }
      val duration = System.currentTimeMillis() - startTime
      duration should be < 2000L
    }).test
  }
}