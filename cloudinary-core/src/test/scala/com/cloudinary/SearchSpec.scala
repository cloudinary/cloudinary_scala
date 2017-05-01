package com.cloudinary

import com.cloudinary.parameters.UploadParameters
import com.cloudinary.response.CustomCoordinate
import org.scalatest.{BeforeAndAfterAll, Inside, Matchers, OptionValues}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import org.scalatest.{Matchers, _}

class SearchSpec extends MockableFlatSpec with Matchers with OptionValues with Inside with BeforeAndAfterAll {
  val testResourcePath = "cloudinary-core/src/test/resources"
  val prefix = "cloudinary_scala"
  val suffix = sys.env.getOrElse("TRAVIS_JOB_ID", scala.util.Random.nextInt(9999).toString)

  val testTag = prefix + "_" + suffix
  val searchTag = testTag + "_search"

  override def beforeAll(): Unit = {
    val options = UploadParameters().
      tags(Set(prefix, testTag, searchTag)).
      context(Map("key" -> "value")).
      customCoordinates(List(CustomCoordinate(10,11,12,13))).
      eager(List(Transformation().w_(100).c_("scale")))
    var (r1, r2, r3) = Await.result(for {
      r1 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options).recover { case _ => "r1 failed" }
      r2 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options).recover { case _ => "r2 failed" }
      r3 <- cloudinary.uploader().upload(s"$testResourcePath/logo.png", options).recover { case _ => "r3 failed" }
    } yield (r1, r2, r3), 20 seconds)
    Thread.sleep(3000)
  }
  override def afterAll(): Unit = {
    cloudinary.api().deleteResourcesByTag(searchTag)
  }

  behavior of "Cloudinary API"

  it should "add expression to query" in {
    var search = cloudinary.search().expression("format:jpg")
    search.toJSON() should equal("{\"expression\":\"format:jpg\"}")
  }
  it should "should add sort_by to query" in {
    var search = cloudinary.search().sortBy("created_at", "asc").sortBy("updated_at", "desc")
    search.toJSON() should equal("{\"sort_by\": [{ \"created_at\":\"asc\"},{\"updated_at\":\"desc\"}]}")
  }
  it should "add max_results to query" in {
    var search = cloudinary.search().maxResults(10)
    search.toJSON() should equal("{\"max_results\":10}")
  }

  it should "allow listing resource_types" in {
    var res = Await.result(cloudinary.search().expression("tags:" + searchTag).execute(), 5 seconds)
    res.total_count should equal(3)
//    should contain("image")
  }

}
