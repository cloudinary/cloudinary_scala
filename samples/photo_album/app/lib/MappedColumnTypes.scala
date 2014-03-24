package lib

import play.api.db.slick.Config.driver.simple._
import scala.slick.profile.RelationalProfile
import java.sql.Timestamp
import org.joda.time.DateTime
import cloudinary.model.CloudinaryResource

object MappedColumnTypes {
  implicit def date2dateTime = MappedColumnType.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    date => new DateTime(date))

  implicit def cloudinaryResourceToString = MappedColumnType.base[CloudinaryResource, String](
    resource => resource.identifier,
    identifier => CloudinaryResource.stored(identifier))
}