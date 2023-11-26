package com.directbooks.http

import cats.effect.Concurrent
import cats.implicits.toSemigroupKOps
import com.directbooks.http.routes.{AccountRoutes, TransactionRoutes}
import com.directbooks.modules.Core
import org.http4s.server.Router

class HttpApi[F[_]: Concurrent](core: Core[F]) {
  private val accountRoutes = AccountRoutes[F](core.accountsAlgebra, core.transactor).routes
  private val transactionRoutes =
    TransactionRoutes[F](core.accountsAlgebra, core.transactionsAlgebra, core.transactor).routes

  val endpoints = Router(
    "/directbooks" -> (accountRoutes <+> transactionRoutes)
  )
}

object HttpApi {
  def apply[F[_]: Concurrent](core: Core[F]) = new HttpApi[F](core)
}
