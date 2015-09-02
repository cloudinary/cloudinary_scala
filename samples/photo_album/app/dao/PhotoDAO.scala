package dao

import scala.concurrent.Future

import java.sql.Timestamp
import javax.inject.Inject
import org.joda.time.DateTime
import models.Photo
import com.cloudinary.Implicits._
import cloudinary.model.{CloudinaryResource, CloudinaryResourceBuilder}

import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.db.NamedDatabase
import slick.driver.JdbcProfile


class PhotoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, 
                          protected val resourceBuilder:CloudinaryResourceBuilder) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  implicit def date2dateTime = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    date => new DateTime(date))

  implicit def cloudinaryResourceToString = MappedColumnType.base[CloudinaryResource, String](
    resource => resource.identifier,
    identifier => resourceBuilder.stored(identifier))

  private val photos = TableQuery[PhotosTable]

  def all(): Future[Seq[Photo]] = db.run(photos.sortBy(p => p.createdAt.desc).result)

  def insert(photo: Photo) = db.run((photos returning photos.map(_.id)) += photo)
  def find(id:Long) :Future[Option[Photo]] = db.run(photos.filter(p => p.id === id).result).map{_.headOption}

  private class PhotosTable(tag: Tag) extends Table[Photo](tag, "photos"){
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def image = column[CloudinaryResource]("image")
    def bytes = column[Int]("bytes")
    def createdAt = column[DateTime]("created_at")

    def * = (id, title, image, bytes, createdAt) <> (Photo.tupled, Photo.unapply)
  }
}