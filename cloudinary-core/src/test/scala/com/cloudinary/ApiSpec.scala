package com.cloudinary

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success
import org.scalatest._
import org.scalatest.matchers.ShouldMatchers
import parameters._
import Implicits._

class ApiSpec extends FlatSpec with ShouldMatchers with OptionValues with Inside with BeforeAndAfterAll {
  lazy val cloudinary = {
    val c = new Cloudinary()
    if (c.getStringConfig("api_key", None).isEmpty) {
      System.err.println("Please setup environment for Upload test to run")
    }
    c
  }

  lazy val api = cloudinary.api()

  override def beforeAll(configMap: Map[String, Any]) {

    val options = UploadParameters().publicId("api_test").
      tags(Set("api_test_tag")).
      context(Map("key" -> "value")).
      eager(List(Transformation().w_(100).c_("scale")))
    Await.result(for {
      r1 <- api.deleteResources(List("api_test", "api_test1", "api_test2", "api_test3", "api_test4", "api_test5", "api_test_by_prefix")).recover { case _ => "r1 failed" }
      r2 <- api.deleteTransformation("api_test_transformation").recover { case _ => "r2 failed" }
      r3 <- api.deleteTransformation("api_test_transformation2").recover { case _ => "r3 failed" }
      r4 <- api.deleteTransformation("api_test_transformation3").recover { case _ => "r4 failed" }
      r5 <- cloudinary.uploader().upload("src/test/resources/logo.png", options).recover { case _ => "r5 failed" }
      r6 <- cloudinary.uploader().upload("src/test/resources/logo.png", options.publicId("api_test1")).recover { case _ => "r6 failed" }
    } yield (r1, r2, r3, r4, r5, r6), 20 seconds)
  }

  behavior of "Cloudinary API"

  it should "allow listing resource_types" in {
    Await.result(api.resourceTypes().map(_.resource_types), 5 seconds) should contain("image")
  }

  it should "allow listing resources" in {
    val v = Await.result(api.resources().map(response => response.resources.find(r => r.public_id == "api_test" && r.`type` == "upload")), 5 seconds)
    v.isDefined should be(true)
  }

  it should "allow listing resources with cursor" in {
    val r1 = Await.result(api.resources(maxResults = 1), 5 seconds)
    r1.resources.size should equal(1)
    r1.next_cursor.isDefined should be(true)
    val r2 = Await.result(api.resources(maxResults = 1, nextCursor = r1.next_cursor), 5 seconds)
    r2.resources.size should equal(1)
    r1.resources(0).public_id should not equal (r2.resources(0).public_id)
  }

  it should "allow listing resources by type" in {
    Await.result(api.resources(`type` = "upload", tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id).toSet should contain("api_test")
        response.resources.map(_.tags).toSet should contain(List("api_test_tag"))
        response.resources.map(_.context).toSet should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow listing resources by prefix" in {
    Await.result(api.resources(`type` = "upload", prefix = "api_test", tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id).foreach(_ should startWith("api_test"))
        response.resources.map(_.tags).toSet should contain(List("api_test_tag"))
        response.resources.map(_.context).toSet should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow specifying direction when listing resources" in {
    Await.result(api.resources(`type` = "upload", prefix = "api_test", direction = Api.ASCENDING), 5 seconds).resources.reverse should equal(
      Await.result(api.resources(`type` = "upload", prefix = "api_test", direction = Api.DESCENDING), 5 seconds).resources)
  }

  it should "allow listing resources by tag" in {
    Await.result(api.resourcesByTag("api_test_tag", tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id).toSet should equal(Set("api_test", "api_test1"))
        response.resources.map(_.tags).toSet should contain(List("api_test_tag"))
        response.resources.map(_.context).toSet should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow listing resources by public id" in {
    Await.result(api.resourcesByIds(List("api_test", "api_test1"), tags = true, context = true).map {
      response =>
        response.resources.map(_.public_id).toSet should equal(Set("api_test", "api_test1"))
        response.resources.map(_.tags).toSet should contain(List("api_test_tag"))
        response.resources.map(_.context).toSet should contain(Map("custom" -> Map("key" -> "value")))
    }, 5 seconds)
  }

  it should "allow getting a resource's metadata" in {
    val resource = Await.result(api.resource("api_test"), 5 seconds)
    resource.public_id should equal("api_test")
    resource.bytes should equal(3381)
    resource.derived.size should equal(1)
  }

  it should "allow deleting derived resource" in {
    val (resource, resourceAfterDelete) = Await.result(for {
      r1 <- cloudinary.uploader().upload("src/test/resources/logo.png", UploadParameters().
        publicId("api_test3").
        eager(List(Transformation().w_(101).c_("scale"))))
      resource <- api.resource("api_test3") if (r1 != null)
      r2 <- api.deleteDerivedResources(resource.derived.headOption.map(_.id).toList) if resource != null
      resourceAfterDelete <- api.resource("api_test3") if (r2 != null)
    } yield (resource, resourceAfterDelete), 5 seconds)
    resource.derived.size should equal(1)
    resourceAfterDelete.derived.size should equal(0)
  }

  it should "allow deleting resources" in {
    Await.result(for {
      r1 <- cloudinary.uploader().upload("src/test/resources/logo.png", UploadParameters().publicId("api_test3"))
      resource <- api.resource("api_test3") if (r1 != null)
      r2 <- api.deleteResources(List("apit_test", "api_test2", "api_test3")) if resource != null
      throwable <- api.resource("api_test3").recover { case e => e } if (r2 != null)
    } yield { throwable }, 5 seconds).isInstanceOf[NotFound] should be(true)
  }

  it should "allow deleting resources by prefix" in {
    Await.result(for {
      r1 <- cloudinary.uploader().upload("src/test/resources/logo.png", UploadParameters().publicId("api_test_by_prefix"))
      resource <- api.resource("api_test_by_prefix") if r1 != null
      r2 <- api.deleteResourcesByPrefix("api_test_by") if resource != null
      throwable <- api.resource("api_test_by_prefix").recover { case e => e } if r2 != null
    } yield { throwable }, 5 seconds).isInstanceOf[NotFound] should be(true)
  }

  it should "allow deleting resources by tags" in {
    Await.result(for {
      r1 <- cloudinary.uploader().upload("src/test/resources/logo.png", UploadParameters().publicId("api_test4").tags(Set("api_test_tag_for_delete")))
      resource <- api.resource("api_test4") if (r1 != null)
      r2 <- api.deleteResourcesByTag("api_test_tag_for_delete") if resource != null
      throwable <- api.resource("api_test4").recover { case e => e } if (r2 != null)
    } yield { throwable }, 5 seconds).isInstanceOf[NotFound] should be(true)
  }

  it should "allow listing tags" in {
    Await.result(api.tags().map(r => r.tags), 5 seconds) should contain("api_test_tag")
  }

  it should "allow listing tags by prefix" in {
    Await.result(api.tags(prefix = "api_test").map(r => r.tags), 5 seconds) should contain("api_test_tag")
    Await.result(api.tags(prefix = "api_test_no_such_tag").map(r => r.tags), 5 seconds) should equal(List())
  }

  it should "allow listing transformations" in {
    val transformation = Await.result(api.transformations().map(_.transformations), 5 seconds).find(t => t.name == "c_scale,w_100")
    transformation should not equal (None)
    transformation.get.used should equal(true)
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
      r1 <- api.createTransformation("api_test_transformation", t)
      transformation <- api.transformationByName("api_test_transformation")
    } yield transformation, 5 seconds)
    transformation.allowed_for_strict should equal(true)
    transformation.info should equal(t)
    transformation.used should equal(false)
  }

  it should "allow deleting named transformation" in {
    Await.result(for {
      r1 <- api.createTransformation("api_test_transformation2", Transformation().c_("scale").w_(103))
      t1 <- api.transformationByName("api_test_transformation2") if r1 != null
      r2 <- api.deleteTransformation("api_test_transformation2") if t1 != null
      throwable <- api.transformationByName("api_test_transformation2").recover { case e => e } if r2 != null
    } yield throwable, 5 seconds).isInstanceOf[NotFound] should equal(true)
  }

  it should "allow unsafe update of named transformation" in {
    val t1 = Transformation().c_("scale").w_(102)
    val t2 = Transformation().c_("scale").w_(103)
    val tr = Await.result(for {
      r1 <- api.createTransformation("api_test_transformation3", t1)
      r2 <- api.updateTransformation("api_test_transformation3", unsafeUpdate = t2) if r1 != null
      tr <- api.transformationByName("api_test_transformation3") if r2 != null
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
    } yield throwable, 5 seconds).isInstanceOf[NotFound] should equal(true)
  }

  it should "support usage API call" in {
    Await.result(api.usage().map(_.last_updated), 5 seconds) should not be (null)
  }

  it should "support ping API call" in {
    Await.result(api.ping().map(_.status), 5 seconds) should equal("ok")
  }
  
  //Remove ignore to test delete all - note use with care!!!
  ignore should "allow deleting all resources" in {
    Await.result(for {
      r1 <- cloudinary.uploader().upload("src/test/resources/logo.png", UploadParameters().publicId("api_test5").eager(List(Transformation().w_(0.2).c_("scale"))))
      resource1 <- api.resource("api_test5") if (r1 != null)
      r2 <- api.deleteAllResources(keepOriginal = true) if resource1 != null
      resource2 <- api.resource("api_test5")
    } yield {
      resource1.derived.length should equal(1)
      resource2.derived.length should equal(0)
    }, 5 seconds)
  }
}
