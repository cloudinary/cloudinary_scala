package com.cloudinary

import java.net.URLEncoder
import java.io.UnsupportedEncodingException

object SmartUrlEncoder {
  def encode(input: String) =
    try {
      URLEncoder.encode(input, "UTF-8").replace("%2F", "/").replace("%3A", ":").replace("+", "%20");
    } catch {
      case e: UnsupportedEncodingException => throw new RuntimeException(e);
    }
}
