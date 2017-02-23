package com.cloudinary

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.{IHTTPSession, Response}

class ServerMock[T](handler: IHTTPSession => Response, performTest: () => T ) extends NanoHTTPD(ServerMock.TEST_SERVER_PORT) {
  def test:T = {
    start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
    val ret = performTest()
    stop()
    ret
  }

  override def serve(session: IHTTPSession): Response = handler(session)
}

object ServerMock {
  val TEST_SERVER_PORT = 53777
  def simpleResponse(body:String) = NanoHTTPD.newFixedLengthResponse(body)
  def fixedHandler(f: () => Response) = (s: IHTTPSession) => f()
}