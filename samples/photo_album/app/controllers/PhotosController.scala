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
  
  val photos = TableQuery[Photos]
  
  val photoForm = Form(
    mapping(
      "title" -> nonEmptyText)(PhotoDetails.apply)(PhotoDetails.unapply))

  val directUploadForm = Form(
    mapping(
      "id" -> ignored[Long](0),
      "title" -> nonEmptyText,
      "image" -> of[CloudinaryResource],
      "bytes" -> number,
      "createdAt" -> ignored(DateTime.now))(Photo.apply)(Photo.unapply))

  def photo(id: Long) = DB.withSession {implicit session => 
    photos.filter(p => p.id === id).firstOption
  }

  def index = Action {
    val ps = DB.withSession{ implicit session => photos.sortBy(p => p.createdAt.desc).list }
    Ok(views.html.index(ps))
  }

  def fresh = Action {
    Ok(views.html.fresh(photoForm))
  }

  def freshDirect = Action {
    Ok(views.html.freshDirect(directUploadForm))
  }

  def freshUnsignedDirect = Action {
    // Preset creation does not really belong here - it's just here for the sample to work. 
    // The preset should be created offline
    val cp = current.plugin[cloudinary.plugin.CloudinaryPlugin].get
    val cld = cp.cloudinary

    val presetName = "sample_" + com.cloudinary.Cloudinary.apiSignRequest(
        Map("api_key" -> cld.getStringConfig("api_key")), cld.getStringConfig("api_secret").get
      ).substring(0, 10)

    cld.api.createUploadPreset(com.cloudinary.response.UploadPreset(presetName, true, UploadParameters().folder("preset_folder")))

    Ok(views.html.freshUnsignedDirect(directUploadForm, presetName))
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
              val photo = Photo(0, photoDetails.title, cr, cr.data.get.bytes.toInt, DateTime.now)
              val newPhotoId = DB.withSession{ implicit session =>
                (photos returning photos.map(_.id)) += photo
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
        val newPhotoId = DB.withSession{ implicit session =>
          (photos returning photos.map(_.id)) += photo
        }
        Ok(views.html.create(photo))
      })
  }
}