package lib

import slick.lifted.MappedTypeMapper
import java.sql.Timestamp
import org.joda.time.DateTime
import slick.lifted.TypeMapper.DateTypeMapper
import cloudinary.model.CloudinaryResource

object TypeMappers {
  implicit def date2dateTime = MappedTypeMapper.base[DateTime, Timestamp](
    dateTime => new Timestamp(dateTime.getMillis),
    date => new DateTime(date))

  implicit def cloudinaryResourceToString = MappedTypeMapper.base[CloudinaryResource, String](
    resource => resource.identifier,
    identifier => CloudinaryResource.stored(identifier))
}