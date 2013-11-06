package com.cloudinary

import java.util.concurrent.atomic.AtomicReference
import com.ning.http.client.AsyncHttpClientConfig
import com.ning.http.client.AsyncHttpClient
import org.json4s._
import org.json4s.native.JsonMethods._
import scala.concurrent.Promise
import com.ning.http.client.Request
import concurrent.ExecutionContext.Implicits.global
import com.cloudinary.response.RawResponse
import java.text.SimpleDateFormat

object HttpClient {
  private val clientHolder: AtomicReference[Option[AsyncHttpClient]] = new AtomicReference(None)

  private[cloudinary] def newClient(): AsyncHttpClient = {

    val asyncHttpConfig = new AsyncHttpClientConfig.Builder()
    new AsyncHttpClient(asyncHttpConfig.build())
  }

  def client: AsyncHttpClient = {
    clientHolder.get.getOrElse({
      // A critical section of code. Only one caller has the opportuntity of creating a new client.
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

  def executeCloudinaryRequest(r: Request) = {
    val result = Promise[JsonAST.JValue]()
    val handler = new AsyncCloudinaryHandler(result)
    client.executeRequest(r, handler)
    result.future
  }

  implicit val formats = new DefaultFormats {
    override def dateFormatter = new SimpleDateFormat("yyyy-MM-dd")
  }

  def executeAndExtractResponse[T](r: Request)(implicit mf: scala.reflect.Manifest[T]) =
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
}