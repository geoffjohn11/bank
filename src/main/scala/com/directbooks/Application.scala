package com.directbooks

import cats.effect.{IO, IOApp, Resource}
import com.directbooks.http.HttpApi
import com.directbooks.modules.Core
import org.http4s.ember.server.EmberServerBuilder

object Application extends IOApp.Simple {

  override def run = {
    (for {
      c       <- Core.apply
      httpApp <- Resource.pure(HttpApi(c))
      server  <- EmberServerBuilder.default[IO].withHttpApp(httpApp.endpoints.orNotFound).build
    } yield server).useForever
  }
}
