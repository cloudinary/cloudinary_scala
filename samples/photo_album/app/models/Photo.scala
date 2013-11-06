package models

import org.joda.time.DateTime
import play.api.db.slick.Config.driver.simple._
import cloudinary.model.CloudinaryResource
import lib.TypeMappers._
import com.cloudinary.Transformation
import com.cloudinary.Implicits._

case class PhotoDetails(title: String)

case class Photo(id:Option[Long], title:String, image:CloudinaryResource, bytes:Int, createdAt:DateTime) {
  def url = image.url()
  def thumbnailUrl = image.url(Transformation().w_(150).h_(150).c_("fit").quality(80))
}

object Photos extends Table[Photo]("photos"){
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def title = column[String]("title")
  def image = column[CloudinaryResource]("image")
  def bytes = column[Int]("bytes")
  def createdAt = column[DateTime]("created_at")
  def * = id.? ~ title ~ image ~ bytes ~ createdAt <> (Photo, Photo.unapply _)
  def forInsert = title ~ image ~ bytes ~ createdAt <> ({ t => Photo(None, t._1, t._2, t._3, t._4)}, { (u: Photo) => Some((u.title, u.image, u.bytes, DateTime.now))})
}