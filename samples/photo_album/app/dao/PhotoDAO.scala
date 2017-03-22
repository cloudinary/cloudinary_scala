package dao

import java.sql.Timestamp
import javax.inject.Inject

import cloudinary.model.{CloudinaryResource, CloudinaryResourceBuilder}
import models.Photo
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}


class PhotoDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, 
                          protected val resourceBuilder:CloudinaryResourceBuilder,
                          implicit val ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  implicit def date2dateTime:BaseColumnType[DateTime] = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    date => new DateTime(date))

  implicit def cloudinaryResourceToString:BaseColumnType[CloudinaryResource] = MappedColumnType.base[CloudinaryResource, String](
    resource => resource.identifier,
    identifier => resourceBuilder.stored(identifier))

  private val photos = TableQuery[PhotosTable]

  def all(): Future[Seq[Photo]] = db.run(photos.sortBy(p => p.createdAt.desc).result)

  def insert(photo: Photo) = db.run((photos returning photos.map(_.id)) += photo)
  def find(id:Long) :Future[Option[Photo]] = db.run(photos.filter(p => p.id === id).result).map{_.headOption}

  private class PhotosTable(tag: Tag) extends Table[Photo](tag, "photos"){
    def id:Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title:Rep[String] = column[String]("title")
    def image:Rep[CloudinaryResource] = column[CloudinaryResource]("image")
    def bytes:Rep[Int] = column[Int]("bytes")
    def createdAt:Rep[DateTime] = column[DateTime]("created_at")

    def * = (id, title, image, bytes, createdAt) <> (Photo.tupled, Photo.unapply)
  }
}