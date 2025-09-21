package com.cloudinary

import java.util.concurrent.atomic.AtomicReference
import java.time.Duration

import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.{Future, Promise}
import org.asynchttpclient.Request

import concurrent.ExecutionContext.Implicits.global
import com.cloudinary.response.RawResponse
import java.text.SimpleDateFormat

/**
  * Instantiate this class to perform HTTP actions.
  */
class HttpClient {
  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  }

  /** holds an instance of the HTTP client. Replace this value if you need to use a custom HttpClient. */
  var client: AsyncHttpClient = HttpClient.client
  private[cloudinary] val clientHolder: AtomicReference[Option[AsyncHttpClient]] = null
  def executeAndExtractResponse[T](r: Request)(implicit mf: scala.reflect.Manifest[T]): Future[T] =
    executeCloudinaryRequest(r).map {
      j =>
        try {

          val r = j.extract[T]
          if (r.isInstanceOf[RawResponse]) r.asInstanceOf[RawResponse].raw = j
          r
        } catch {
          case e: MappingException => throw new MappingException(e.msg + "\nIn:\n" + j.toString)
        }
    }

  def executeCloudinaryRequest(r: Request) = {
    val result = Promise[JsonAST.JValue]()
    val handler = new AsyncCloudinaryHandler(result)
    client.executeRequest(r, handler)
    result.future
  }

}


object HttpClient {
  private[cloudinary] val clientHolder: AtomicReference[Option[AsyncHttpClient]] = new AtomicReference(None)

  private[cloudinary] def newClient(): AsyncHttpClient = {

    val asyncHttpConfig = new DefaultAsyncHttpClientConfig.Builder()
      .setUserAgent(Cloudinary.USER_AGENT)
      .setReadTimeout(Duration.ofMillis(-1))
      .build()
    new DefaultAsyncHttpClient(asyncHttpConfig)
  }

  /**
    * provides a singleton of AsyncHttpClient. Normally this instance will be used by all API calls.
    * @return
    */
  def client: AsyncHttpClient = {
    clientHolder.get.getOrElse({
      // A critical section of code. Only one caller has the opportunity of creating a new client.
      synchronized {
        clientHolder.get match {
          case None => {
            val client = newClient()
            clientHolder.set(Some(client))
            client
          }
          case Some(client) => client
        }

      }
    })
  }



}
