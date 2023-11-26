package com.directbooks.http.routes

import cats.effect.Concurrent
import cats.implicits._
import doobie.implicits._
import com.directbooks.core.Accounts
import com.directbooks.domain.AccountInfo
import doobie.util.transactor.Transactor
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe.jsonEncoder
import org.http4s.dsl._
import org.http4s.server._

class AccountRoutes[F[_]: Concurrent] private (accounts: Accounts[F], xa: Transactor[F])
    extends Http4sDsl[F] {

  private val findAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / LongVar(accountId) =>
      for {
        acct <- accounts.find(accountId).transact(xa)
        res  <- acct.fold(NotFound("Account does not exist"))(acct => Ok(acct.asJson))
      } yield res
  }

  private val createAccountRoute: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root =>
    for {
      balance <- req.as[AccountInfo]
      id      <- accounts.create(balance.startingBalance).transact(xa)
      acct    <- accounts.find(id).transact(xa)
      res     <- acct.fold(NotFound("Account does not exist"))(acct => Ok(acct.asJson))
    } yield res
  }

  val routes = Router(
    "/account" -> (findAccountRoute <+> createAccountRoute)
  )
}

object AccountRoutes {
  def apply[F[_]: Concurrent](accounts: Accounts[F], xa: Transactor[F]) =
    new AccountRoutes[F](accounts, xa)
}
