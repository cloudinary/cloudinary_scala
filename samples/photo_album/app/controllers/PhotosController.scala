package controllers

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api._
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.Play.current
import play.api.db.slick.DB
import play.api.db.slick.Config.driver.simple._
import models._
import play.api.data._
import play.api.data.Forms._
import cloudinary.model.CloudinaryResource
import cloudinary.model.CloudinaryResource.preloadedFormatter
import org.joda.time.DateTime
import com.cloudinary.parameters.UploadParameters
import com.cloudinary.Implicits._

object PhotosController extends Controller {

  val photoForm = Form(
    mapping(
      "title" -> nonEmptyText)(PhotoDetails.apply)(PhotoDetails.unapply))

  val directUploadForm = Form(
    mapping(
      "id" -> ignored[Option[Long]](None),
      "title" -> nonEmptyText,
      "image" -> of[CloudinaryResource],
      "bytes" -> number,
      "createdAt" -> ignored(DateTime.now))(Photo.apply)(Photo.unapply))

  def photos = DB.withSession { implicit session: Session =>
    Query(Photos).sortBy(p => p.createdAt.desc).list
  }

  def photo(id: Long) = DB.withSession { implicit session: Session =>
    Query(Photos).filter(p => p.id === id).firstOption
  }

  def index = Action {
    val ps = photos
    Ok(views.html.index(ps))
  }

  def fresh = Action {
    Ok(views.html.fresh(photoForm))
  }

  def freshDirect = Action {
    Ok(views.html.freshDirect(directUploadForm))
  }

  def create = Action.async { implicit request =>
    photoForm.bindFromRequest.fold(
      formWithErrors => future { BadRequest(views.html.fresh(formWithErrors)) },
      photoDetails => {
        val body = request.body.asMultipartFormData
        val resourceFile = body.get.file("photo")
        if (resourceFile.isEmpty) {
          val formWithErrors = photoForm.withError(FormError("photo", "Must supply photo"))
          future { BadRequest(views.html.fresh(formWithErrors)) }
        } else {
          CloudinaryResource.upload(resourceFile.get.ref.file, UploadParameters().faces(true).colors(true).imageMetadata(true).exif(true)).map {
            cr =>
              val photo = Photo(None, photoDetails.title, cr, cr.data.get.bytes.toInt, DateTime.now)
              val newPhotoId = DB.withSession { implicit session: Session =>
                Photos.forInsert returning Photos.id insert photo
              }
              Ok(views.html.create(photo, cr.data))
          }
        }
      })
  }

  def createDirect = Action { implicit request =>
    directUploadForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.freshDirect(formWithErrors)),
      photo => {
        val newPhotoId = DB.withSession { implicit session: Session =>
          Photos.forInsert returning Photos.id insert photo
        }
        Ok(views.html.create(photo))
      })
  }
}