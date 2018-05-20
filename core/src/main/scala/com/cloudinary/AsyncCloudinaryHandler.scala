package com.cloudinary

import scala.concurrent.Promise

import _root_.com.ning.http.client.{
  AsyncHandler,
  AsyncCompletionHandler,
  HttpResponseStatus,
  Response
}

import org.json4s._
import org.json4s.native.JsonMethods._

abstract class ApiException(message: String) extends Exception(message)

case class BadRequest(message: String) extends ApiException(message)

case class AuthorizationRequired(message: String) extends ApiException(message)

case class NotAllowed(message: String) extends ApiException(message)

case class NotFound(message: String) extends ApiException(message)

case class AlreadyExists(message: String) extends ApiException(message)

case class RateLimited(message: String) extends ApiException(message)

case class GeneralError(message: String) extends ApiException(message)

class AsyncCloudinaryHandler[JValue](result: Promise[JsonAST.JValue]) extends AsyncCompletionHandler[Unit] {
  @volatile private var status: HttpResponseStatus = _

  val CLOUDINARY_API_ERROR_CLASSES = Map(
    400 -> classOf[BadRequest],
    401 -> classOf[AuthorizationRequired],
    403 -> classOf[NotAllowed],
    404 -> classOf[NotFound],
    409 -> classOf[AlreadyExists],
    420 -> classOf[RateLimited],
    500 -> classOf[GeneralError])

  override def onThrowable(t: Throwable) = {
    result.failure(t)
  }

  private def getHttpException(message: Option[String], statusCode:Int):ApiException = {
    val exceptionClass =
      CLOUDINARY_API_ERROR_CLASSES.getOrElse(
        statusCode,
        CLOUDINARY_API_ERROR_CLASSES(500))
    exceptionClass.getConstructor(classOf[String]).newInstance(message.getOrElse("Unknown Reason"))
  }

  def onCompleted(response: Response): Unit = {
    val eitherJson: Either[Throwable, JsonAST.JValue] = try {
      Right(parse(response.getResponseBody()))
    } catch {
      case e: Exception =>
        Left(new RuntimeException("Invalid JSON response from server " + e.getMessage()))
    }

    eitherJson match {
      case Right(json) if (response.getStatusCode() / 100 == 2) => result.success(json)
      case Right(json) =>
        val JString(error) = json \\ "message"
        result.failure(getHttpException(Some(error), response.getStatusCode()))
      case Left(e) => result.failure(e)
    }
  }
}