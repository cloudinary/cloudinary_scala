package com.cloudinary

case class Transformation(val transformations:List[Map[String, Any]] = List(Map[String, Any]())) {

	private val defaultResponsiveWidthTransformation:String = "c_limit,w_auto"

	protected def transformation:Map[String, Any] = transformations.last

	def this(transformation:Transformation) {		
		this(transformation.transformations)
	}
	
	/**
	 * Chain another transformation
	 */
	def chain(another:Transformation):Transformation = { 
	  new Transformation(transformations.:+(another.transformation))
	}
	def /(another:Transformation) = chain(another:Transformation)
	
	/**
	 * Append a new transformation to the end
	 */
	def chain():Transformation = { 
	  new Transformation(transformations ++ List(Map[String, Any]()))
	}
	def / = chain()

	protected def param(key:String, value:Any):Transformation = {
	  val nl = transformations.last + (key -> value) 
	  new Transformation(transformations.dropRight(1) ++ List(nl))
	}
	
	
	/**
	 * Apply a pre-defined named transformation of the given name. 
	 */
	def named(value:String*) = param("transformation", value)
	def t_(value:String*) = param("transformation", value)
	
	/**
	 * The required width in pixels of a transformed image or an overlay. 
	 * Can be specified separately or together with the height value.
	 */
	def width(value:Int) = param("width", value)
	def w_(value:Int) = width(value)
	
	/**
	 * The required width in percent (0.1 = 10%) of a transformed image or an overlay. 
	 * Can be specified separately or together with the height value.
	 */
	def width(value:Float) = param("width", value)
	def w_(value:Float) = width(value)
	def width(value:Double) = param("width", value)
	def w_(value:Double) = width(value)

	/**
	 * The required width in pixels of a transformed image or an overlay. 
	 * Can be specified separately or together with the height value.
	 */
	def width(value:String) = value match {
		case "auto" => 
			param("width", value).responsiveWidth(true)
		case i => param("width", Integer.parseInt(i, 10))
	}
	def w_(value:String) = width(value)
	
	/**
	 * The required height in pixels of a transformed image or an overlay.
	 * Can be specified separately or together with the width value.
	 */
	def height(value:Int) = param("height", value)
	def h_(value:Int) = height(value)
	
	/**
	 * The required height in percent (0.1 = 10%) of a transformed image or an overlay.
	 * Can be specified separately or together with the width value.
	 */
	def height(value:Float) = param("height", value)
	def h_(value:Float) = height(value)
	def height(value:Double) = param("height", value)
	def h_(value:Double) = height(value)
	
	/**
	 * A crop mode that determines how to transform the image for fitting into the desired 
	 * width & height dimensions.
	 */
	def crop(value:String) = param("crop", value)
	def c_(value:String) = crop(value)
	
	/**
	 * Defines the background color to use instead of transparent background areas when 
	 * converting to JPG format.
	 */
	def background(value:String) = param("background", value)
	def b_(value:String) = background(value)
	
	/**
	 * Apply a filter or an effect on an image. The value includes the name of the effect 
	 * and an additional parameter that controls the behavior of the specific effect.
	 */
	def effect(value:String) = param("effect", value)
	def effect(e:String, p:Any):Transformation = effect(s"$e:$p")
	def e_(value:String) = effect(value)
	def e_(e:String, p:Any) = effect(e, p)
	
	/**
	 * Rotate an image by the given degrees.
	 */
	def angle(value:Int) = param("angle", List(value.toString))
	def a_(value:Int) = angle(value)
	
	/**
	 * Rotate or flip an image by the given degrees or automatically according to its 
	 * orientation or available meta-data. 
	 * Multiple modes can be applied.
	 */
	def angle(value:String*) = param("angle", value)
	def a_(value:String*) = param("angle", value)

	/**
	 * Adjust opacity to the given percentage.
	 */
	def opacity(value:Int) = param("opacity", value)
	def o_(value:Int) = opacity(value)
	
	/**
	 * Add a solid border around the image. 
	 */
	def border(width:Int, color:String) = param("border", s"${width}px_solid_${color.replaceFirst("^#", "rgb:")}")
	def bo_(width:Int, color:String) = border(width, color)
	
	/**
	 * Horizontal position for custom-coordinates based cropping, overlay placement and 
	 * certain region related effects.
	 */
	def x(value:Any) = param("x", value)
	def x_(value:Any) = x(value)
	
	/**
	 * Vertical position for custom-coordinates based cropping, overlay placement and 
	 * certain region related effects.
	 */
	def y(value:Any) = param("y", value)
	def y_(value:Any) = y(value)
	
	/**
	 * Round the corners of an image.
	 */
	def radius(value:Int) = param("radius", value)
	def r_(value:Int) = radius(value)
	
	/**
	 *  Make image completely circular or oval (ellipse).
	 */
	def circled() = param("radius", "max")
	
	/**
	 * Control the JPG compression quality. 1 is the lowest quality and 100 is the highest. 
	 * The default is the original image's quality or 90% if not available. Reducing quality 
	 * generates JPG images much smaller in file size.
	 */
	def quality(value:Any) = param("quality", value)
	def q_(value:Any) = quality(value)
	
	/**
	 * Specify the public ID of a place-holder image to use if the requested image or social 
	 * network picture does not exist.
	 */
	def defaultImage(value:String) = param("default_image", value)
	def d_(value:String) = defaultImage(value)
	
	/**
	 * Decides which part of the image to keep while 'crop', 'pad' and 'fill' crop modes are used. 
	 * For overlays, this decides where to place the overlay.
	 */
	def gravity(value:String) = param("gravity", value)
	def g_(value:String) = gravity(value)
	
	def colorSpace(value:String) = { param("color_space", value) }
	
	def prefix(value:String) = param("prefix", value)
	def p_(value:String) = prefix(value)
	
	/**
	 * Add an overlay image over the base image. You can control the dimension and position of 
	 * the overlay using the width, height, x, y and gravity parameters. The identifier can be a 
	 * public ID of an uploaded image or a specific image kind, public ID and settings.
	 */
	def overlay(value:String) = param("overlay", value)
	def l_(value:String) = overlay(value)
	
	/**
	 * Add an underlay image below a base partially-transparent image. You can control the 
	 * dimensions and position of the underlay using the width, height, x, y and gravity parameters. 
	 * The identifier can be a public ID of an uploaded image or a specific image kind, public ID 
	 * and settings.
	 */
	def underlay(value:String) = param("underlay", value)
	def u_(value:String) = underlay(value)
	
	/**
	 * Force format conversion to the given image format for remote 'fetch' URLs that already have a 
	 * different format as part of their URLs.
	 */
	def fetchFormat(value:String) = {
	  param("fetch_format", value)
	}
	def f_(value:String) = fetchFormat(value)
	
	/**
	 * Control the density to use while converting a PDF document to images. 
	 * (range: 50-300, default: 150)
	 */
	def density(value:Int) = {
	  if (value < 50 || value > 300) throw new IllegalArgumentException("Value is out of range (50:300)")
	  param("density", value) 
	}
	def dn_(value:Int) = density(value) 
	
	/**
	 * Given a multi-page PDF document, generate an image of a single page using the given index.
	 */
	def page(value:Int) = { param("page", value) }
	def pg_(value:Int) = page(value)
	
	/**
	 * 
	 */
	def delay(value:Any) = param("delay", value)
	def dl_(value:Any) = delay(value)
	
	def rawTransformation(value:String) = param("raw_transformation", value)
	
	/**
	 * Set one or more flags that alter the default transformation behavior.
	 */
	def flags(value:String*) = param("flags", value)
	def fl_(value:String*) = param("flags", value)

	/**
	 * DPR - Device Pixel Ratio
	 */
	def dpr(value:Int) = param("dpr", value)
	def dpr(value:Float) = param("dpr", value)
	def dpr(value:String) = param("dpr", value)

	/**
	 * Sets explicitly whether this transformation has responsive width
	 */
	def responsiveWidth(value: Boolean) = param("responsive_width", value)
	
	/**
	 * Returns the detected width to be embedded in a HTML IMG tag based on 
	 * transformation definitions alone
	 */
	def htmlWidth:Option[Int] = getPixelSize("width")
	
	/**
	 * Returns the detected height to be embedded in a HTML IMG tag based on 
	 * transformation definitions alone
	 */
	def htmlHeight:Option[Int] = getPixelSize("height")

	def generate():String = transformations.map(generate).mkString("/")

	protected def generate(options:Map[String, Any]):String = {
		val angle = optionalList(options.get("angle"))
		val background = options.get("background").map(_.toString.replaceFirst("^#", "rgb:")).getOrElse("")
		
		val flags = optionalList(options.get("flags"))
		val named = optionalList(options.get("transformation"))
		
		val params = Map[String, String](
				"b" -> background,
				"a" -> angle,
				"fl" -> flags,
				"t" -> named
			)
			
		val simpleParams = Seq(
			"w", "width", "h", "height", "x", "x", "y", "y", "r", "radius", "d", "default_image", 
			"g", "gravity", "cs", "color_space", "p", "prefix", "l", "overlay", "u", "underlay", 
			"f", "fetch_format", "dn", "density", "pg", "page", "dl", "delay", "e", "effect", 
			"bo", "border", "q", "quality", "c", "crop", "dpr", "dpr", "o", "opacity"
		).grouped(2).map {
		  p => p.head -> options.getOrElse(p.last, "")
		}.filter(p => p._2 != "" && p._2 != null).toMap

		val components = (params ++ simpleParams)
			.filter(p => p._2 != "" && p._2 != null)
			.map(p => p._1 + "_" + p._2)
			.toList
			.sorted
		
		components.mkString(",") 
	}
	
	protected def noHtmlSizes(transformation:Map[String, Any]) = {
	  val hasLayer = transformation.get("overlay").filterNot(_.toString().isEmpty).isDefined || 
				        transformation.get("underlay").filterNot(_.toString().isEmpty).isDefined
	  hasLayer || transformation.get("angle").filterNot(_.toString().isEmpty).isDefined || 
		  	transformation.get("crop").filter(s => s == "fit" || s == "limit").isDefined
	}
	
	protected def getPixelSize(key:String):Option[Int] = {
	  var foundSizeNuller = false
	  val size = transformations.reverse.collectFirst{
	    case t if noHtmlSizes(t) => 
	      foundSizeNuller = true
	      0
	    case t if hasInt(t, key) => t(key).asInstanceOf[Int]
	  }
	  if (foundSizeNuller) None else size
	}

	private[cloudinary] def isResponsive = transformations.exists{_.get("responsive_width") match {
		case Some(true) => true
		case _ => false
	}}

	private[cloudinary] def hasWidthAuto = transformations.exists{_.get("width") match {
		case Some("auto") => true
		case _ => false
	}}

	private[cloudinary] def isHiDPI = transformations.exists{_.get("dpi") match {
		case Some("auto") => true
		case _ => false
	}}
	
	protected def hasInt(transformation:Map[String, Any], key:String) = transformation.get(key) match {
	    case Some(x:Int) => true
	    case _ => false
	}
	
	private def optionalList(o:Option[_], sep:String = ".") = o match {
	  case Some(l:Iterable[_]) => l.map(_.toString).mkString(sep)
	  case _ => ""
	} 
	
}

class EagerTransformation(
    val format:String,
    transformations:List[Map[String, Any]] = List(Map[String, Any]())
    ) extends Transformation(transformations) {
}
