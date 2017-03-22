package controllers

import javax.inject._

import cloudinary.model.{CloudinaryResource, CloudinaryResourceBuilder}
import com.cloudinary.parameters.UploadParameters
import dao._
import models._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import play.api.i18n.I18nSupport
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class PhotosController @Inject() (
  photoDao:PhotoDAO, 
  cloudinaryResourceBuilder: CloudinaryResourceBuilder,
  cc: ControllerComponents,
  webJarAssets:WebJarAssets) extends AbstractController(cc) with I18nSupport {
  
  implicit val cld:com.cloudinary.Cloudinary = cloudinaryResourceBuilder.cld
  implicit val webJarAssetsImpl:WebJarAssets = webJarAssets
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

  def index = Action.async(parse.anyContent){ implicit request:RequestHeader =>
    photoDao.all().map{photos => Ok(views.html.index(photos))}
  }

  def fresh = Action(parse.anyContent){ implicit request:RequestHeader =>
    Ok(views.html.fresh(photoForm))
  }

  def freshDirect = Action(parse.anyContent){ implicit request:RequestHeader =>
    Ok(views.html.freshDirect(directUploadForm))
  }

  def freshUnsignedDirect = Action(parse.anyContent){ implicit request:RequestHeader =>
    // Preset creation does not really belong here - it's just here for the sample to work. 
    // The preset should be created offline

    val presetName = "sample_" + com.cloudinary.Cloudinary.apiSignRequest(
        Map("api_key" -> cld.getStringConfig("api_key")), cld.getStringConfig("api_secret").get
      ).substring(0, 10)

    cld.api.createUploadPreset(com.cloudinary.response.UploadPreset(presetName, true, UploadParameters().folder("preset_folder")))

    Ok(views.html.freshUnsignedDirect(directUploadForm, presetName))
  }

  def create = Action.async(parse.multipartFormData) { implicit request =>
    implicit val messages = cc.messagesApi.preferred(request)
    val photoDetails = request.body
    request.body.file("photo").map { photo =>
      cloudinaryResourceBuilder.upload(photo.ref.path.toFile, UploadParameters().faces(true).colors(true).imageMetadata(true).exif(true)).flatMap {
        cr =>
          val photo = Photo(0, request.body.asFormUrlEncoded("title").headOption.getOrElse("Empty Title"), cr, cr.data.get.bytes.toInt, DateTime.now)
          photoDao.insert(photo).map{
            _ => Ok(views.html.create(photo, cr.data))
          }
      }
    }.getOrElse {
      Future{Redirect(routes.PhotosController.index).flashing(
                      "error" -> "Missing file")}
    }
  }

  def createDirect = Action.async(parse.form(directUploadForm)) { implicit request =>
    implicit val messages = cc.messagesApi.preferred(request)
    val photo = request.body
    photoDao.insert(photo).map {
      _ => Ok(views.html.create(photo))
    }
  }
}