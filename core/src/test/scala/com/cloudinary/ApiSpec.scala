package com.cloudinary

import java.net.URLEncoder
import java.text.{DateFormat, SimpleDateFormat}
import java.util
import java.util.{Date, TimeZone}

import com.cloudinary.Implicits._
import com.cloudinary.parameters._
import com.cloudinary.response._
import com.ning.http.client._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, _}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import collection.JavaConverters._

class ApiSpec extends MockableFlatSpec with Matchers with OptionValues with Inside with BeforeAndAfterAll{

  private lazy val api = cloudinary.api()
  private lazy val uploader = cloudinary.uploader()

  private val testResourcePath = "cloudinary-core/src/test/resources"
  private val apiTag = testTag + "_api"

  private val apiTest1 = prefix + "1" +suffix

  private val apiTest2 = prefix + "2" +suffix
  private val apiTest3 = prefix + "3" +suffix
  private val apiTest4 = prefix + "4" +suffix
  private val apiTest5 = prefix + "5" +suffix

  private val apiTestUploadPreset = prefix + "UploadPreset" + suffix
  private val apiTestUploadPreset2 = prefix + "UploadPreset2" + suffix
  private val apiTestUploadPreset3 = prefix + "UploadPreset3" + suffix
  private val apiTestUploadPreset4 = prefix + "UploadPreset4" + suffix

  private val apiTestTransformation = testId + "Transformation" + suffix
  private val apiTestTransformation2 = testId + "Transformation2" + suffix
  private val apiTestTransformation3 = testId + "Transformation3" + suffix

  override def beforeAll(): Unit = {
    super.beforeAll()
    val options = UploadParameters().
      tags(Set(prefix, testTag, apiTag)).
      context(Map("key" -> "value")).
      customCoordinates(List(CustomCoordinate(10,11,12,13))).
      eager(List(Transformation().w_(100).c_("scale")))
    Await.result(for {
    r5 <- uploader.upload(s"$testResourcePath/logo.png", options.publicId(testId)).recover { case _ => "r5 failed" }
    r6 <- uploader.upload(s"$testResourcePath/logo.png", options.publicId(apiTest1)).recover { case _ => "r6 failed" }
    } yield (r5, r6), 20 seconds)
  }
  override def afterAll() {
    super.afterAll()
    Await.result(for {
      r1 <- api.deleteResourcesByTag(apiTag).recover { case _ => "r1 failed" }
      r2 <- api.deleteTransformation(apiTestTransformation).recover { case _ => "r2 failed" }
      r3 <- api.deleteTransformation(apiTestTransformation2).recover { case _ => "r3 failed" }
      r4 <- api.deleteTransformation(apiTestTransformation3).recover { case _ => "r4 failed" }
      r7 <- api.deleteUploadPreset(apiTestUploadPreset).recover { case _ => "r7 failed" }
      r8 <- api.deleteUploadPreset(apiTestUploadPreset2).recover { case _ => "r8 failed" }
      r9 <- api.deleteUploadPreset(apiTestUploadPreset3).recover { case _ => "r9 failed" }
      r10 <- api.deleteUploadPreset(apiTestUploadPreset4).recover { case _ => "r10 failed" }
    } yield (r1, r2, r3, r4, r7, r8, r9, r10), 20 seconds)
  }

  behavior of "Cloudinary API"

  it should "allow listing resource_types" in {
    Await.result(api.resourceTypes().map(_.resource_types), 5 seconds) should contain("image")
  }

  it should "allow listing resources" in {
    val v = Await.result(api.resources(maxResults = 500).map(response => response.resources.find(r => r.public_id == testId && r.`type` == "upload")), 10 seconds)
    v.isDefined should be(true)
  }

  it should "allow listing resources with cursor" in {
    val cursor = "OJNASGONQG0230JGV0JV3Q0IDVO"
    val (provider, api) = mockApi()
    (provider.execute _) expects where { (request: Request, _) => {
      val params = getQuery(request)
      params.contains(("next_cursor", cursor))
    }
    }
    api.resources(maxResults = 1, nextCursor = cursor)
  }

  it should "allow listing resources by type" in {
    Await.result(api.resources(`type` = "upload", prefix = prefix, tags = true, context = true, maxResults = 500).map {
      response =>
        response.resources.map(_.public_id) should contain(testId)
        response.resources.map(_.tags) should contain(List(prefix, testTag, apiTag))
        response.resources.map(_.context) should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow listing resources by prefix" in {
    Await.result(api.resources(`type` = "upload", prefix = testId, tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id).foreach(_ should startWith(testId))
        response.resources.map(_.tags) should contain(List(prefix, testTag, apiTag))
        response.resources.map(_.context) should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow specifying direction when listing resources" in {
    Await.result(api.resourcesByTag(apiTag, direction = Api.ASCENDING), 5 seconds).resources.reverse should equal(
      Await.result(api.resourcesByTag(apiTag, direction = Api.DESCENDING), 5 seconds).resources)
  }

  it should "allow listing resources by tag" in {
    Await.result(api.resourcesByTag(apiTag, tags = true, context = true, maxResults = 500).map {
      response =>
        response.resources.map(_.public_id) should contain allOf (testId, apiTest1)
        response.resources.map(_.tags) should contain(List(prefix, testTag, apiTag))
        response.resources.map(_.context) should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow listing resources by public id" in {
    Await.result(api.resourcesByIds(List(testId, apiTest1), tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id) should contain only(testId, apiTest1)
        response.resources.map(_.tags) should contain(List(prefix, testTag, apiTag))
        response.resources.map(_.context) should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow listing resources by start date" in {
    val df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz")
    df.setTimeZone(TimeZone.getTimeZone("UTC"))
    val (provider, api) = mockApi()
    val startAt = df.parse("22 Aug 2016 07:57:34 UTC")
    (provider.execute _) expects where { (request: Request, *) => {
      val params = getQuery(request)
      params.contains(("start_at", "22 Aug 2016 07:57:34 UTC GMT")) &&
        params.contains(("direction", "asc"))
    }
    }
    api.resources(`type` = "upload", startAt = Some(startAt), direction = Some(Api.ASCENDING))
  }

  // remove the ignore if the account used for testing supports filenameContains parameter
  ignore should "allow listing resources by filenameContains" in {
    val (resources1, resources2, resources3) = Await.result(for {
      rootUploadResponse <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId("sample_test_contains"))
      topUploadResponse <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().folder("top_folder").publicId("sample_test_contains"))
      subUploadResponse <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().folder("top_folder/sub_folder").publicId("Amphibian_test_contains"))
      folderMatchUploadResponse <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().folder("top_folder/amphetamines").publicId("benzedrine_test_contains"))
      resources1Response <- api.resources(`type` = "upload", prefix = "top_folder/", filenameContains = "amp") if rootUploadResponse != null && topUploadResponse != null && subUploadResponse != null && folderMatchUploadResponse != null
      resources2Response <- api.resources(`type` = "upload", prefix = "top_folder/", filenameContains = "amp", includeSubfolders = false) if rootUploadResponse != null && topUploadResponse != null && subUploadResponse != null && folderMatchUploadResponse != null
      resources3Response <- api.resources(`type` = "upload", filenameContains = "amp", includeSubfolders = false) if rootUploadResponse != null && topUploadResponse != null && subUploadResponse != null && folderMatchUploadResponse != null
    } yield (resources1Response.resources, resources2Response.resources, resources3Response.resources), 5 seconds)
    resources1.map(_.public_id) should contain allOf("top_folder/sample_test_contains", "top_folder/sub_folder/Amphibian_test_contains")
    resources1.map(_.public_id) should not contain allOf("sample_test_contains", "top_folder/amphetamines/benzedrine_test_contains")

    resources2.map(_.public_id) should contain("top_folder/sample_test_contains")
    resources2.map(_.public_id) should not contain allOf("sample_test_contains", "top_folder/amphetamines/benzedrine_test_contains", "top_folder/sub_folder/Amphibian_test_contains")

    resources3.map(_.public_id) should contain("sample_test_contains")
    resources3.map(_.public_id) should not contain allOf("top_folder/sample_test_contains", "top_folder/amphetamines/benzedrine_test_contains", "top_folder/sub_folder/Amphibian_test_contains")
  }

  it should "allow getting a resource's metadata" in {
    val resource = Await.result(api.resource(testId, coordinates = true), 5 seconds)
    resource.public_id should equal(testId)
    resource.bytes should equal(3381)
    resource.derived.size should equal(1)
    resource.customCoordinates should equal(List(CustomCoordinate(10,11,12,13)))
  }

  it should "allow deleting derived resource" in {
    val (resource, resourceAfterDelete) = Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().
        publicId(apiTest3).
        eager(List(Transformation().w_(101).c_("scale"))))
      resource <- api.resource(apiTest3) if (r1 != null)
      r2 <- api.deleteDerivedResources(resource.derived.headOption.map(_.id).toList) if resource != null
      resourceAfterDelete <- api.resource(apiTest3) if (r2 != null)
    } yield (resource, resourceAfterDelete), 5 seconds)
    resource.derived.size should equal(1)
    resourceAfterDelete.derived.size should equal(0)
  }

  it should "allow deleting resources" in {
    Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId(apiTest3))
      resource <- api.resource(apiTest3) if (r1 != null)
      r2 <- api.deleteResources(List("apit_test", apiTest2, apiTest3)) if resource != null
      throwable <- api.resource(apiTest3).recover { case e => e } if (r2 != null)
    } yield { throwable }, 5 seconds).isInstanceOf[NotFound] should be(true)
  }

  it should "allow deleting resources by prefix" in {
    val apiTestByPrefix = s"${prefix}_${suffix}_ByPrefix"
    val apiTestByPrefixId = s"${apiTestByPrefix}_id"
    Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId(apiTestByPrefixId))
      resource <- api.resource(apiTestByPrefixId) if r1 != null
      r2 <- api.deleteResourcesByPrefix(apiTestByPrefix) if resource != null
      throwable <- api.resource(apiTestByPrefixId).recover { case e => e } if r2 != null
    } yield { throwable }, 5 seconds).isInstanceOf[NotFound] should be(true)
  }

  private val apiTestTagForDelete: String = prefix + "TagForDelete" + suffix

  it should "allow deleting resources by tags" in {
    Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId(apiTest4).tags(Set(apiTestTagForDelete)))
      resource <- api.resource(apiTest4) if (r1 != null)
      r2 <- api.deleteResourcesByTag(apiTestTagForDelete) if resource != null
      throwable <- api.resource(apiTest4).recover { case e => e } if (r2 != null)
    } yield { throwable }, 5 seconds) shouldBe a[NotFound]
  }

  it should "allow listing tags" in {
    Await.result(api.tags(maxResults = 500).map(r => r.tags), 5 seconds) should contain(testTag)
  }

  it should "allow listing tags by prefix" in {
    Await.result(api.tags(prefix = testId).map(r => r.tags), 5 seconds) should contain(testTag)
    Await.result(api.tags(prefix = testId + "NoSuchTag").map(r => r.tags), 5 seconds) should equal(List())
  }

  it should "allow listing transformations" in {
    val transformation = Await.result(api.transformations().map(_.transformations), 5 seconds).find(t => t.name == "c_scale,w_100")
    transformation should not equal (None)
    transformation.get.used should equal(true)
  }

  it should "allow listing transformations with next_cursor" in {
    val (provider, api) = mockApi()
    (provider.execute _) expects where { (request: Request, *) => {
        request.getQueryParams.contains(new Param("next_cursor", "1234567"))
      }
    }
    api.transformations(nextCursor = "1234567")
  }

  it should "allow getting transformation metadata" in {
    val t = Transformation().c_("scale").w_(100)
    val transformation = Await.result(api.transformation(t), 5 seconds)
    transformation.info should equal(t)
  }

  it should "allow updating transformation allowed_for_strict" in {
    Await.result(for {
      r1 <- api.updateTransformation("c_scale,w_100", allowedForStrict = true)
      t1 <- api.transformation(Transformation().c_("scale").w_(100)).map(_.allowed_for_strict) if r1 != null
      r2 <- api.updateTransformation("c_scale,w_100", allowedForStrict = false) if t1
      t2 <- api.transformation(Transformation().c_("scale").w_(100)).map(_.allowed_for_strict) if r2 != null
    } yield t2, 5 seconds) should equal(false)
  }

  it should "allow creating named transformation" in {
    val t = Transformation().c_("scale").w_(102)
    val transformation = Await.result(for {
      r1 <- api.createTransformation(apiTestTransformation, t)
      transformation <- api.transformationByName(apiTestTransformation)
    } yield transformation, 5 seconds)
    transformation.allowed_for_strict should equal(true)
    transformation.info should equal(t)
    transformation.used should equal(false)
  }


  it should "allow getting transformation metadata with next_cursor" in {
    val t = Transformation().c_("scale").w_(100)
    val (provider, api) = mockApi()
    inSequence {
      (provider.execute _) expects where {
        (request: Request, *) => {
          request.getQueryParams.contains(new Param("next_cursor", "1234567")) &&
            request.getUrl.matches(".+/" + apiTestTransformation + "?.+")
        }
      }
      (provider.execute _) expects where {
        (request: Request, *) => {
          request.getQueryParams.contains(new Param("next_cursor", "1234567")) &&
            request.getUrl.matches(".+/" + apiTestTransformation + "?.+")
        }
      }
      (provider.execute _) expects where {
        (request: Request, *) => {
          request.getQueryParams.contains(new Param("max_results", "111")) &&
            !request.getQueryParams.asScala.exists(p => p.getName == "next_cursor") &&
            request.getUrl.matches(".+/" + apiTestTransformation + "?.+")
        }
      }
    }
    api.transformationByName(apiTestTransformation, "1234567")
    api.transformationByName(apiTestTransformation, nextCursor = "1234567")
    api.transformationByName(apiTestTransformation, 111)
  }

  it should "allow listing transformation by name with next_cursor" in {
    val (provider, api) = mockApi()
    provider.execute _ expects where {
      (request: Request, handler: AsyncHandler[Nothing]) => {
        request.getQueryParams.contains(new Param("next_cursor", "1234567"))
      }
    }
    api.transformationByName(apiTestTransformation, nextCursor = "1234567")
  }

  it should "allow deleting named transformation" in {
    Await.result(for {
      r1 <- api.createTransformation(apiTestTransformation2, Transformation().c_("scale").w_(103))
      t1 <- api.transformationByName(apiTestTransformation2) if r1 != null
      r2 <- api.deleteTransformation(apiTestTransformation2) if t1 != null
      throwable <- api.transformationByName(apiTestTransformation2).recover { case e => e } if r2 != null
    } yield throwable, 5 seconds) shouldBe a[NotFound]
  }

  it should "allow unsafe update of named transformation" in {
    val t1 = Transformation().c_("scale").w_(102)
    val t2 = Transformation().c_("scale").w_(103)
    val tr = Await.result(for {
      r1 <- api.createTransformation(apiTestTransformation3, t1)
      r2 <- api.updateTransformation(apiTestTransformation3, unsafeUpdate = t2) if r1 != null
      tr <- api.transformationByName(apiTestTransformation3) if r2 != null
    } yield tr, 5 seconds)
    tr.info should equal(t2)
    tr.used should equal(false)
  }

  it should "allow deleting implicit transformation" in {
    val t = Transformation().c_("scale").w_(100)
    Await.result(for {
      tr <- api.transformation(t)
      r1 <- api.deleteTransformation(t) if tr != null
      throwable <- api.transformation(t).recover { case e => e } if r1 != null
    } yield throwable, 5 seconds) shouldBe a[NotFound]
  }

  it should "allow creating and listing upload_presets" in {
    Await.result(for {
      r1 <- api.createUploadPreset(UploadPreset(apiTestUploadPreset, settings = UploadParameters().folder("folder")))
      r2 <- api.createUploadPreset(UploadPreset(apiTestUploadPreset2, settings = UploadParameters().folder("folder2"))) if r1 != null
      r3 <- api.createUploadPreset(UploadPreset(apiTestUploadPreset3, settings = UploadParameters().folder("folder3"))) if r2 != null
      presets <- api.uploadPresets() if r3 != null
      d1 <- api.deleteUploadPreset(apiTestUploadPreset) if presets != null
      d2 <- api.deleteUploadPreset(apiTestUploadPreset2) if presets != null
      d3 <- api.deleteUploadPreset(apiTestUploadPreset3) if presets != null
    } yield presets, 5 seconds).presets.take(3).map{_.name} should equal(List(apiTestUploadPreset3, apiTestUploadPreset2, apiTestUploadPreset))
  }

  it should "allow getting a single upload_preset" in {
    val (presetName, preset) = Await.result(for {
      result <- api.createUploadPreset(
        UploadPreset(name = null, unsigned = true,
          settings = UploadParameters().folder("folder").transformation(
            Transformation().width(100).crop("scale"))
            .tags(Set("a", "b", "c"))
            .context(Map("a" -> "b", "c" -> "d"))))
      presetResponse <- api.uploadPreset(result.name)
      deleteResult <- api.deleteUploadPreset(presetResponse.preset.name)
    } yield (result.name, presetResponse.preset), 5 seconds)
    preset.name should equal(presetName)
    preset.unsigned should be(true)
    preset.settings("folder") should equal("folder")
    preset.settings("transformation") should equal(Transformation(Map("width" -> 100, "crop" -> "scale") :: Nil))
    preset.settings("context") should equal(Map("a" -> "b", "c" -> "d"))
    preset.settings("tags") should equal(Set("a", "b", "c"))
  }

  it should "allow deleting upload_presets" in {
    Await.result(for {
      result <- api.createUploadPreset(UploadPreset(name = apiTestUploadPreset4,  settings = UploadParameters().folder("folder")))
      presetResult <- api.uploadPreset(result.name)
      deleteResult <- api.deleteUploadPreset(result.name) if presetResult != null
      e <- api.uploadPreset(result.name).recover{case e => e} if deleteResult != null
    } yield e, 5 seconds) shouldBe a[NotFound]
  }

  it should "allow updating upload_presets" in {
    val (presetName, updatedPreset) = Await.result(for {
      result <- api.createUploadPreset(UploadPreset(name = null, settings = UploadParameters().folder("folder")))
      preset <- api.uploadPreset(result.name).map(_.preset)
      updatedPresetResult <- api.updateUploadPreset(UploadPreset(result.name, true, preset.settings.colors(true).disallowPublicId(true)))
      presetResult <- api.uploadPreset(result.name) if updatedPresetResult != null
      deleteResult <- api.deleteUploadPreset(result.name) if presetResult != null
    } yield (result.name, presetResult.preset), 5 seconds)
    updatedPreset.name should equal(presetName)
    updatedPreset.unsigned shouldBe true
    updatedPreset.settings should equal(UploadParameters().folder("folder").colors(true).disallowPublicId(true))
  }

  it should "support usage API call" in {
    Await.result(api.usage().map(_.last_updated), 5 seconds) should not be (null)
  }

  it should "support ping API call" in {
    Await.result(api.ping().map(_.status), 5 seconds) should equal("ok")
  }

  it should "support setting manual moderation status" in {
    Await.result(for  {
      uploadResult <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().moderation("manual"))
      apiResult <- cloudinary.api.update(uploadResult.public_id, UpdateParameters().moderationStatus(ModerationStatus.approved))
    } yield apiResult, 10.seconds).moderation.head.status should equal(ModerationStatus.approved)
  }

  it should "support requesting raw conversion" in {
    val error = Await.result(for {
      uploadResult <- uploader.upload(s"$testResourcePath/logo.png")
      e <- cloudinary.api.update(uploadResult.public_id, UpdateParameters().rawConvert("illegal")).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("Illegal value")
  }

  it should "support requesting categorization" in {
    val error = Await.result(for {
      uploadResult <- uploader.upload(s"$testResourcePath/logo.png")
      e <- cloudinary.api.update(uploadResult.public_id, UpdateParameters().categorization("illegal")).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("Illegal value")
  }

  it should "support requesting detection" in {
    val error = Await.result(for {
      uploadResult <- uploader.upload(s"$testResourcePath/logo.png")
      e <- cloudinary.api.update(uploadResult.public_id, UpdateParameters().detection("illegal")).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("Illegal value")
  }

  it should "support requesting auto_tagging" in {
    val error = Await.result(for {
      uploadResult <- uploader.upload(s"$testResourcePath/logo.png")
      e <- cloudinary.api.update(uploadResult.public_id, UpdateParameters().autoTagging(0.5)).recover{case e => e}
    } yield e, 10.seconds)
    error.asInstanceOf[BadRequest].message should include("Must use")
  }

  it should "support listing by moderation kind and value" in {
    Await.result(for {
      result1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().moderation("manual"))
      result2 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().moderation("manual"))
      result3 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().moderation("manual"))
      apiresult1 <- cloudinary.api.update(result1.public_id, UpdateParameters().moderationStatus(ModerationStatus.approved))
      apiresult2 <- cloudinary.api.update(result2.public_id, UpdateParameters().moderationStatus(ModerationStatus.rejected))
      approved <- cloudinary.api.resourcesByModeration(status = ModerationStatus.approved, maxResults = 1000, moderations = true) if result3 != null && apiresult1 != null && apiresult2 != null
      rejected <- cloudinary.api.resourcesByModeration(status = ModerationStatus.rejected, maxResults = 1000, moderations = true) if result3 != null && apiresult1 != null && apiresult2 != null
      pending <- cloudinary.api.resourcesByModeration(status = ModerationStatus.pending, maxResults = 1000, moderations = true) if result3 != null && apiresult1 != null && apiresult2 != null
    } yield {
      approved.resources.map(_.public_id) should contain(result1.public_id)
      approved.resources.forall(_.moderationStatus == Some(ModerationStatus.approved)) should be(true)
      approved.resources.map(_.public_id) should not contain(result2.public_id)
      approved.resources.map(_.public_id) should not contain(result3.public_id)
      rejected.resources.map(_.public_id) should contain(result2.public_id)
      rejected.resources.forall(_.moderationStatus == Some(ModerationStatus.rejected)) should be(true)
      rejected.resources.map(_.public_id) should not contain(result1.public_id)
      rejected.resources.map(_.public_id) should not contain(result3.public_id)
      pending.resources.map(_.public_id) should contain(result3.public_id)
      pending.resources.forall(_.moderationStatus == Some(ModerationStatus.pending)) should be(true)
      pending.resources.map(_.public_id) should not contain(result1.public_id)
      pending.resources.map(_.public_id) should not contain(result2.public_id)
    }, 20.seconds)
  }

  // For this test to work, 'Auto-create folders' should be enabled in the Upload Settings,
  // and the account should be empty of folders.
  // Comment out this line if you really want to test it.
  ignore should "support listing folders" in {
    Await.result(for {
      result1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId("test_folder1/item"))
      result2 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId("test_folder2/item"))
      result3 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId("test_folder1/test_subfolder1/item"))
      result4 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId("test_folder1/test_subfolder2/item"))
      apiresult1 <- cloudinary.api.rootFolders if result1 != null && result2 != null && result3 != null && result4 != null
      apiresult2 <- cloudinary.api.subfolders("test_folder1") if result1 != null && result2 != null && result3 != null && result4 != null
      apiresult3 <- cloudinary.api.subfolders("test_folder").recover{case e => e} if result1 != null && result2 != null && result3 != null && result4 != null
    } yield {
      apiresult1.folders.map(_.name) should contain("test_folder1")
      apiresult1.folders.map(_.name) should contain("test_folder2")
      apiresult2.folders.map(_.path) should contain("test_folder1/test_subfolder1")
      apiresult2.folders.map(_.path) should contain("test_folder1/test_subfolder2")
      apiresult3.isInstanceOf[NotFound] should be(true)
    }, 10 seconds)
  }

  //Remove ignore to test delete all - note use with care!!!
  ignore should "allow deleting all resources" in {
    Await.result(for {
      r1 <- uploader.upload(s"$testResourcePath/logo.png", UploadParameters().publicId(apiTest5).eager(List(Transformation().w_(0.2).c_("scale"))))
      resource1 <- api.resource(apiTest5) if (r1 != null)
      r2 <- api.deleteAllResources(keepOriginal = true) if resource1 != null
      resource2 <- api.resource(apiTest5)
    } yield {
      resource1.derived.length should equal(1)
      resource2.derived.length should equal(0)
    }, 5 seconds)
  }
}
