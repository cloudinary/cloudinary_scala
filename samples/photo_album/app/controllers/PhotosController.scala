package controllers

import javax.inject._

import scala.concurrent._
import ExecutionContext.Implicits.global

import org.joda.time.DateTime

import play.api._
import play.api.mvc.Controller
import play.api.mvc.Action
import play.api.i18n.I18nSupport
import play.api.i18n.MessagesApi

import play.api.data._
import play.api.data.Forms._

import cloudinary.model.{CloudinaryResource, CloudinaryResourceBuilder}

import com.cloudinary.parameters.UploadParameters
import com.cloudinary.Implicits._

import dao._
import models._

class PhotosController @Inject() (
  photoDao:PhotoDAO, 
  cloudinaryResourceBuilder: CloudinaryResourceBuilder, 
  val messagesApi: MessagesApi) extends Controller with I18nSupport {
  
  implicit val cld:com.cloudinary.Cloudinary = cloudinaryResourceBuilder.cld
  import cloudinaryResourceBuilder.preloadedFormatter

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

  def photo(id: Long) =  photoDao.find(id)

  def index = Action.async { implicit request =>
    photoDao.all().map{photos => Ok(views.html.index(photos))}
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

    val presetName = "sample_" + com.cloudinary.Cloudinary.apiSignRequest(
        Map("api_key" -> cld.getStringConfig("api_key")), cld.getStringConfig("api_secret").get
      ).substring(0, 10)

    cld.api.createUploadPreset(com.cloudinary.response.UploadPreset(presetName, true, UploadParameters().folder("preset_folder")))

    Ok(views.html.freshUnsignedDirect(directUploadForm, presetName))
  }

  def create = Action.async { implicit request =>
    photoForm.bindFromRequest.fold(
      formWithErrors => Future { BadRequest(views.html.fresh(formWithErrors)) },
      photoDetails => {
        val body = request.body.asMultipartFormData
        val resourceFile = body.get.file("photo")
        if (resourceFile.isEmpty) {
          val formWithErrors = photoForm.withError(FormError("photo", "Must supply photo"))
          Future { BadRequest(views.html.fresh(formWithErrors)) }
        } else {
          cloudinaryResourceBuilder.upload(resourceFile.get.ref.file, UploadParameters().faces(true).colors(true).imageMetadata(true).exif(true)).flatMap {
            cr =>
              val photo = Photo(0, photoDetails.title, cr, cr.data.get.bytes.toInt, DateTime.now)
              photoDao.insert(photo).map{
                _ => Ok(views.html.create(photo, cr.data))
              }
          }
        }
      })
  }

  def createDirect = Action.async { implicit request =>
    directUploadForm.bindFromRequest.fold(
      formWithErrors => Future {BadRequest(views.html.freshDirect(formWithErrors))},
      photo => {
        photoDao.insert(photo).map{
          _ => Ok(views.html.create(photo))
        }
      })
  }
}