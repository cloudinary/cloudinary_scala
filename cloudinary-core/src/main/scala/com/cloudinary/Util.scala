package com.cloudinary

object Util {
  def definedMap(m: Map[String, Any]): Map[String, Any] = m.map(p => p match {
    case (k, v: Option[_]) => k -> v
    case (k, v) => k -> Option(v)
  }).collect {
    case kv if kv._2.isDefined => kv._1 -> kv._2.get
  }
}