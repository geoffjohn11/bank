package com.directbooks.http.routes

import io.circe.generic.auto._
import io.circe.syntax._
import doobie.implicits._
import org.http4s.circe.CirceEntityCodec._
import org.http4s._
import org.http4s.dsl._
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.impl._
import org.http4s.server._
import cats._
import cats.effect.kernel.Concurrent
import cats.implicits._
import com.directbooks.domain.{Account, Transaction, TransactionInfo}
import com.directbooks.core.{Accounts, Transactions}
import com.directbooks.http.routes.TransactionRoutes.placeholder
import doobie.ConnectionIO
import doobie.util.transactor.Transactor

class TransactionRoutes[F[_]: Concurrent] private (
    accounts: Accounts[F],
    transactions: Transactions[F],
    xa: Transactor[F]
) extends Http4sDsl[F] {

  private val transactionHistoryRoute: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "history" / LongVar(accountId) =>
      for {
        t <- transactions.findByAccountId(accountId).transact(xa)
        res <- Ok(t.asJson)
      } yield res
    }

  private val createTransactionRoute: HttpRoutes[F] = {

  def validBalance(balance: BigDecimal, tInfo: TransactionInfo) = {
    tInfo.description match {
      case "withdrawl" if tInfo.amount.compareTo(balance) <= 0 => true
      case "withdrawl" => false
      case _ => true
    }
  }

    def updateAccount(account: Account, tInfo: TransactionInfo) = {
      tInfo.description match{
        case "deposit" =>  accounts.update(account.id, account.balance + tInfo.amount)
        case "withdrawl" =>  accounts.update(account.id, account.balance - tInfo.amount)
      }
    }

  def performTransfer(account: Account, tInfo: TransactionInfo) = {
    for {
      tid <- transactions.create(
        Transaction(placeholder, tInfo.accountId, tInfo.amount, tInfo.description, "success", placeholder)
      )
      _ <- updateAccount(account, tInfo)
      details <- transactions.findByTransactionId(tid)
    } yield details
  }

  def failTransfer(tInfo: TransactionInfo) = {
    for {
      _ <- transactions.create(
        Transaction(placeholder, tInfo.accountId, tInfo.amount, tInfo.description, "failed", placeholder)
      )
    } yield List[Transaction]()
  }

  def transactionTransfer(tranInfo: TransactionInfo) = {
    (for {
      account <- accounts.find(tranInfo.accountId)
      transactions <- account match {
        case None => List[Transaction]().pure[ConnectionIO]
        case Some(accnt) =>
          if (validBalance(accnt.balance, tranInfo)) performTransfer(accnt, tranInfo)
          else failTransfer(tranInfo)
      }
    } yield transactions).transact(xa)
  }

  HttpRoutes.of[F] { case req@POST -> Root =>
    for {
      tInfo <- req.as[TransactionInfo]
      res <- tInfo.description match {
      case "withdrawl" | "deposit" =>
         for{
           account <- accounts.find(tInfo.accountId).transact(xa)
           resp <- account.fold(NotFound("Account does not exist")) { _ =>
             transactionTransfer(tInfo).flatMap {
               case Nil => UnprocessableEntity("Withdrawl exceeds balance")
               case details => Created(details.headOption.toList.asJson)
             }
           }
         }  yield resp
      case desc => BadRequest(s"transaction description must be either 'withdrawl' or 'deposit', input was [${desc}]")
    }
    } yield res
  }
}

  val routes = Router(
    "/transaction" -> (transactionHistoryRoute <+> createTransactionRoute)
  )
}

object TransactionRoutes {
  def apply[F[_]: Concurrent](
      accounts: Accounts[F],
      transactions: Transactions[F],
      xa: Transactor[F]
  ) = new TransactionRoutes[F](accounts, transactions, xa)

  val placeholder = -1
}
