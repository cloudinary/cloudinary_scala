package com.cloudinary

// See http://stackoverflow.com/a/4605816
object SmartUrlEncoder {
  val unsafe = " %$&+,;=?@<>#%".toSet
  val hex = "0123456789ABCDEF"

  def encode(input: String) =
    input flatMap {
      ch =>
        if (isUnsafe(ch)) {
          List('%', hex(ch / 16), hex(ch % 16))
        } else {
          List(ch)
        }
    }

  private def toHex(ch: Int) =
    (if (ch < 10) {
      '0' + ch.toString
    } else {
      'A' + (ch - 10).toString
    })

  private def isUnsafe(ch: Char): Boolean =
    if (ch > 128 || ch < 0) {
      true
    } else unsafe.contains(ch)
}
