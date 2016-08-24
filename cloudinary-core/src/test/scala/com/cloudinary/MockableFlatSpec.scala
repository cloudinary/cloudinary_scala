package com.cloudinary

import java.net.URLDecoder

import com.ning.http.client.multipart.StringPart
import com.ning.http.client.{AsyncHttpClient, AsyncHttpClientConfig, AsyncHttpProvider, Request}
import org.scalamock.clazz.Mock
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterEach, FlatSpec}

import scala.collection.JavaConverters._

class MockableFlatSpec extends FlatSpec with MockFactory with BeforeAndAfterEach{

  /**
    * Mock the AsyncHttpProvider so that calls do not invoke the server side.
    * Expectations can be set on the execute method of AsyncHttpProvider.
    * @return the mocked instance
    */
  def mockHttp(): AsyncHttpProvider = {
    val mockProvider: AsyncHttpProvider = mock[AsyncHttpProvider]
    val asyncHttpConfig = new AsyncHttpClientConfig.Builder()
    asyncHttpConfig.setUserAgent(Cloudinary.USER_AGENT)
    HttpClient.clientHolder.set(Some(new AsyncHttpClient(mockProvider, asyncHttpConfig.build())))
    mockProvider
  }

  /**
    * Get the parts (parameters) passed to the request. Use this for non GET requests
    * @param request
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
    * @param request
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
    super.beforeEach()
  }

}
