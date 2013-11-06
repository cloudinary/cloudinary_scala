package com.cloudinary

import scala.language.implicitConversions

object Implicits {
	implicit def booleanToOptionalBoolean(b:Boolean):Option[Boolean] = Option(b)
	implicit def stringToOptionalString(s:String):Option[String] = Option(s)
	implicit def intToOptionalInt(i:Int):Option[Int] = Option(i)
	implicit def transformationToOptionalTransformation(t:Transformation):Option[Transformation] = Option(t)
}