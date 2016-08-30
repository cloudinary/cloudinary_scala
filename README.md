[![Build Status](https://travis-ci.org/cloudinary/cloudinary_scala.svg)](https://travis-ci.org/cloudinary/cloudinary_scala)

Cloudinary
==========

Cloudinary is a cloud service that offers a solution to a web application's entire image management pipeline. 

Easily upload images to the cloud. Automatically perform smart image resizing, cropping and conversion without installing any complex software. 
Integrate Facebook or Twitter profile image extraction in a snap, in any dimension and style to match your website’s graphics requirements. 
Images are seamlessly delivered through a fast CDN, and much much more. 

Cloudinary offers comprehensive APIs and administration capabilities and is easy to integrate with any web application, existing or new.

Cloudinary provides URL and HTTP based APIs that can be easily integrated with any Web development framework. 

For Scala, Cloudinary provides a library for simplifying the integration even further. A Scala Play plugin is provided as well.

## Setup ######################################################################

The Play 2.4 branch is not currently published to a Maven repository. To use it in your project you can run `sbt publishLocal`.

To use it, add the following dependency to your `build.sbt`:
    
    resolvers += Resolver.file("Local Ivy", file(Path.userHome + "/.ivy2/local"))(Resolver.ivyStylePatterns)
    
    libraryDependencies += "com.cloudinary" %% "cloudinary-core-scala" % "0.9.9-SNAPSHOT"

If using the [Play 2.4](http://www.playframework.com/) you can add:

    "com.cloudinary" %% "cloudinary-scala-play" % "0.9.9-SNAPSHOT"

In your controller inject an instance of `CloudinaryResourceBuilder` and make sure to declare an implicit reference to the enclosed `Cloudinary` instance and import `preloadedFormatter`. For example:

```scala
class PhotosController @Inject() (cloudinaryResourceBuilder: CloudinaryResourceBuilder) extends Controller {
  
  implicit val cld:com.cloudinary.Cloudinary = cloudinaryResourceBuilder.cld
  import cloudinaryResourceBuilder.preloadedFormatter
  ..
}
```

To use it in a view or to use one of the included view helpers have your Twirl view accept an implicit `Cloudinary` instance. For example:

```
@(implicit cld:com.cloudinary.Cloudinary)
...
<img src="@url("officialchucknorrispage", Set('format -> "png", 'type -> "facebook", 
    'transformation -> Transformation().h_(95).w_(95).c_("thumb").g_("face").e_("sepia").r_(20)./.a_(10)))">
```

## Try it right away

Sign up for a [free account](https://cloudinary.com/users/register/free) so you can try out image transformations and seamless image delivery through CDN.

*Note: Replace `demo` in all the following examples with your Cloudinary's `cloud name`.*  

Accessing an uploaded image with the `sample` public ID through a CDN:

    http://res.cloudinary.com/demo/image/upload/sample.jpg

![Sample](https://res.cloudinary.com/demo/image/upload/w_0.4/sample.jpg "Sample")

Generating a 150x100 version of the `sample` image and downloading it through a CDN:

    http://res.cloudinary.com/demo/image/upload/w_150,h_100,c_fill/sample.jpg

![Sample 150x100](https://res.cloudinary.com/demo/image/upload/w_150,h_100,c_fill/sample.jpg "Sample 150x100")

Converting to a 150x100 PNG with rounded corners of 20 pixels: 

    http://res.cloudinary.com/demo/image/upload/w_150,h_100,c_fill,r_20/sample.png

![Sample 150x150 Rounded PNG](https://res.cloudinary.com/demo/image/upload/w_150,h_100,c_fill,r_20/sample.png "Sample 150x150 Rounded PNG")

For plenty more transformation options, see our [image transformations documentation](http://cloudinary.com/documentation/image_transformations).

Generating a 120x90 thumbnail based on automatic face detection of the Facebook profile picture of Bill Clinton:
 
    http://res.cloudinary.com/demo/image/facebook/c_thumb,g_face,h_90,w_120/billclinton.jpg
    
![Facebook 90x120](https://res.cloudinary.com/demo/image/facebook/c_thumb,g_face,h_90,w_120/billclinton.jpg "Facebook 90x200")

For more details, see our documentation for embedding [Facebook](http://cloudinary.com/documentation/facebook_profile_pictures) and [Twitter](http://cloudinary.com/documentation/twitter_profile_pictures) profile pictures. 


## Usage

### Configuration

#### When using the client library directly

Each request for building a URL of a remote cloud resource must have the `cloud_name` parameter set. 
Each request to our secure APIs (e.g., image uploads, eager sprite generation) must have the `api_key` and `api_secret` parameters set. 
See [API, URLs and access identifiers](http://cloudinary.com/documentation/api_and_access_identifiers) for more details.

Setting the `cloud_name`, `api_key` and `api_secret` parameters can be done either directly in each call to a Cloudinary method, 
by initializing the Cloudinary object, or by using the CLOUDINARY_URL environment variable / system property.

The entry point of the library is the Cloudinary object. 

```scala
val cloudinary = new Cloudinary()
```

Here's an example of setting the configuration parameters programatically:

```scala
val cloudinary = new Cloudinary(Map(
  "cloud_name" -> "n07t21i7",
  "api_key" -> "123456789012345",
  "api_secret" -> "abcdeghijklmnopqrstuvwxyz12"
))
```

Another example of setting the configuration parameters by providing the CLOUDINARY_URL value to the constructor:

```scala
val cloudinary = new Cloudinary("cloudinary://123456789012345:abcdeghijklmnopqrstuvwxyz12@n07t21i7")
```

#### When using the Play plugin

Add cloudinary block in your `application.conf`:

    cloudinary = {
      cloud_name = n07t21i7
      api_key = 123456789012345
      api_secret = abcdeghijklmnopqrstuvwxyz12
    }

### Embedding and transforming images

Any image uploaded to Cloudinary can be transformed and embedded using powerful view helper methods:

The following example generates the url for accessing an uploaded `sample` image while transforming it to fill a 100x150 rectangle:

```scala
cloudinary.url().transformation(Transformation().width(100).height(150).
                                crop("fill")).
                 generate("sample.jpg")
```

Another example, emedding a smaller version of an uploaded image while generating a 90x90 face detection based thumbnail (note the shorter syntax): 

```scala
cloudinary.url().transformation(Transformation().w_(90).h_(90).
                                c_("thumb").g_("face")).
                 generate("woman.jpg")
```

You can provide either a Facebook name or a numeric ID of a Facebook profile or a fan page.  
             
Embedding a Facebook profile to match your graphic design is very simple:

```scala
cloudinary.url().type("facebook").
                 transformation(Transformation().width(130).height(130).
                                crop("fill").gravity("north_west")).
                 generate("billclinton.jpg")
```
                           
Same goes for Twitter:

```scala
cloudinary.url().type("twitter_name").generate("billclinton.jpg")
```

### Upload

Assuming you have your Cloudinary configuration parameters defined (`cloud_name`, `api_key`, `api_secret`), uploading to Cloudinary is very simple.
    
The following example uploads a local JPG to the cloud: 
    
```scala
cloudinary.uploader().upload("my_picture.jpg")
```

The uploaded image is assigned a randomly generated public ID. The image is immediately available for download through a CDN:

```scala
cloudinary.url().generate("abcfrmo8zul1mafopawefg.jpg")
```
        
    http://res.cloudinary.com/demo/image/upload/abcfrmo8zul1mafopawefg.jpg

You can also specify your own public ID:    

```scala
import com.cloudinary.parameters.UploadParameters
import com.cloudinary.Implicits._

cloudinary.uploader().upload("http://www.example.com/image.jpg", 
                             UploadParameters(publicId = "sample_remote"))

cloudinary.url().generate("sample_remote.jpg")
```

    http://res.cloudinary.com/demo/image/upload/sample_remote.jpg
        
### Play Helpers

Import using:

    @import cloudinary.views.html.helper._

#### url

Returns the URL to Cloudinary encoding transformation and URL options:

Usage:

    @url("sample", Set('transformation -> Transformation().width(100).height(100).crop("fill"), 'format -> "png"))
    
    # http://res.cloudinary.com/cloud_name/image/upload/c_fill,h_100,w_100/sample.png

  
## Additional resources ##########################################################

Additional resources are available at:

* [Website](http://cloudinary.com)
* [Documentation](http://cloudinary.com/documentation)
* [Image transformations documentation](http://cloudinary.com/documentation/image_transformations)
* [Upload API documentation](http://cloudinary.com/documentation/upload_images)

## Support

You can [open an issue through GitHub](https://github.com/cloudinary/cloudinary_scala/issues).

Contact us [http://cloudinary.com/contact](http://cloudinary.com/contact)

Stay tuned for updates, tips and tutorials: [Blog](http://cloudinary.com/blog), [Twitter](https://twitter.com/cloudinary), [Facebook](http://www.facebook.com/Cloudinary).

## License #######################################################################

Released under the MIT license.
