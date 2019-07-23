package com.cloudinary

import com.cloudinary.Api.ASCENDING
import com.cloudinary.parameters.UploadParameters
import com.cloudinary.response.CustomCoordinate
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, OptionValues}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class SearchSpec extends MockableFlatSpec with Matchers with OptionValues with Inside with BeforeAndAfterAll {
  val testResourcePath = "cloudinary-core/src/test/resources"
  val searchTag = testTag + "_search"

  override def beforeAll(): Unit = {
    super.beforeAll()
    val options = UploadParameters().
      tags(Set(prefix, testTag, searchTag)).
      context(Map("key" -> "value")).
      customCoordinates(List(CustomCoordinate(10, 11, 12, 13))).
      eager(List(Transformation().w_(100).c_("scale")))
    val tagsSet = Set(searchTag)
    val (r1, r2, r3) = Await.result(for {
      r1 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options.publicId(searchTag + "1").tags(tagsSet)).map(_.public_id).recover { case _ => "r1 failed" }
      r2 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options.publicId(searchTag + "2").tags(tagsSet)).map(_.public_id).recover { case _ => "r2 failed" }
      r3 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options.publicId(searchTag + "3").tags(tagsSet)).map(_.public_id).recover { case _ => "r3 failed" }
      r3 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options.publicId(searchTag + "4").tags(tagsSet).folder("perú")).map(_.public_id).recover { case _ => "r4 failed" }
    } yield (r1, r2, r3), 20 seconds)
    Thread.sleep(3000)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    cloudinary.api().deleteResourcesByTag(searchTag)
  }

  behavior of "Cloudinary API"

  //  it should "add expression to query" in {
  //    var search = cloudinary.search().expression("format:jpg")
  //    search.toJSON() should equal("{\"expression\":\"format:jpg\"}")
  //  }
  //  it should "should add sort_by to query" in {
  //    var search = cloudinary.search().sortBy("created_at", "asc").sortBy("updated_at", "desc")
  //    search.toJSON() should equal("{\"sort_by\": [{ \"created_at\":\"asc\"},{\"updated_at\":\"desc\"}]}")
  //  }
  //  it should "add max_results to query" in {
  //    var search = cloudinary.search().maxResults(10)
  //    search.toJSON() should equal("{\"max_results\":10}")
  //  }

  it should "allow listing resource_types" in {
    var res = Await.result(cloudinary.search().expression("tags:" + searchTag).execute, 5 seconds)
    res.totalCount should equal(4)
  }
  it should "return resource by public id" in {
    var res = Await.result(cloudinary.search().expression("public_id:" + searchTag + "1").execute, 5 seconds)
    res.resources.size should equal(1)
  }
  it should "paginate resources limited by tag and ordered by ascending public_id" in {
    var res = Await.result(cloudinary.search().maxResults(1).expression("tags:" + searchTag).sortBy("public_id", ASCENDING).execute, 5 seconds)
    res.resources.size should equal(1)
    res.resources.last.publicId should equal(searchTag + "1")
    res.totalCount should equal(4)

    res = Await.result(cloudinary.search().maxResults(1).nextCursor(res.nextCursor.get).expression("tags:" + searchTag).sortBy("public_id", ASCENDING).execute, 5 seconds)
    res.resources.size should equal(1)
    res.resources.last.publicId should equal(searchTag + "2")
    res.totalCount should equal(4)

    res = Await.result(cloudinary.search().maxResults(1).nextCursor(res.nextCursor.get).expression("tags:" + searchTag).sortBy("public_id", ASCENDING).execute, 5 seconds)
    res.resources.size should equal(1)
    res.resources.last.publicId should equal(searchTag + "3")
    res.totalCount should equal(4)

  }

  it should "include context" in {
    var res = Await.result(cloudinary.search().expression("tags:" + searchTag).withField("context").execute, 5 seconds)
    res.resources.foreach(r => r.context.keys should equal(Set("key")))
  }

  it should "allow unicode characters in search query" in {
    val res = Await.result(cloudinary.search().expression("folder=perú AND tags: " + searchTag).execute, 5 seconds)
    res.resources.size should equal(1)
  }
}