package com.cloudinary

import java.net.URLDecoder

import com.ning.http.client.multipart.StringPart
import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig, AsyncHttpProvider, Request}
import org.scalamock.clazz.Mock
import org.scalamock.scalatest.MockFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.BeforeAndAfterEach

import scala.collection.JavaConverters._

class MockableFlatSpec extends AnyFlatSpec with MockFactory with BeforeAndAfterEach{
  protected val prefix = "cloudinary_scala"
  protected val suffix = sys.env.getOrElse("TRAVIS_JOB_ID", (10000 + scala.util.Random.nextInt(89999)).toString)
  protected val testId = s"${prefix}_$suffix"
  protected val testTag = s"${testId}_tag"

  lazy val cloudinary = {
    val c = new Cloudinary()
    if (c.getStringConfig("api_key", None).isEmpty) {
      System.err.println("Please setup environment for Upload test to run")
    }
    c
  }

  /**
    * Mock the AsyncHttpProvider so that calls do not invoke the server side.
    * Expectations can be set on the execute method of AsyncHttpProvider.
    * @return the mocked instance
    */
  def mockHttp() = {
    val mockProvider: AsyncHttpProvider = mock[AsyncHttpProvider]
    val asyncHttpConfig = new AsyncHttpClientConfig.Builder()
    asyncHttpConfig.setUserAgent(Cloudinary.USER_AGENT)
    (mockProvider, new AsyncHttpClient(mockProvider, asyncHttpConfig.build()))
  }

  /**
    * Returns an instance of [[com.cloudinary.Api Api]] with a mocked [[com.ning.http.client.AsyncHttpProvider AsyncHttpProvider]]
    */
  def mockApi() = {
    val api = cloudinary.api()
    val (mockProvider, client) = mockHttp()
    api.httpclient.client = client
    (mockProvider, api)
  }


  /**
    * Returns an instance of [[com.cloudinary.Uploader Uploader]] with a mocked [[com.ning.http.client.AsyncHttpProvider AsyncHttpProvider]]
    */
  def mockUploader() = {
    val uploader = cloudinary.uploader()
    val (mockProvider, client) = mockHttp()
    uploader.httpclient.client = client
    (mockProvider, uploader)
  }


  /**
    * Get the parts (parameters) passed to the request. Use this for non GET requests
    * @param request the HTTP request being processed
    * @return an array of tuples in the form of (name, value)
    */
  def getParts(request: Request): scala.collection.mutable.Buffer[(String, String)] = {
    request.getParts.asScala.map(p => {
      val sp = p.asInstanceOf[StringPart]
      (sp.getName, sp.getValue)
    })
  }

  /**
    * Get the parts (parameters) passed to the request. Use this for GET requests and others that use URL Query parameters
    * @param request the HTTP request being processed
    * @return an array of tuples in the form of (name, value)
    */

  def getQuery(request: Request): scala.collection.mutable.Buffer[(String, String)] = {
    request.getQueryParams.asScala.map(p => {
      (p.getName, URLDecoder.decode(p.getValue, "UTF-8"))
    })
  }

  override def afterEach() {
    // reset the http client in case a mock has been used
    HttpClient.clientHolder.set(None)
    super.afterEach()
  }

}
