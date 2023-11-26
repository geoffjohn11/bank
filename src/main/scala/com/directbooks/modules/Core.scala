package com.directbooks.modules

import cats.effect.kernel.MonadCancelThrow
import cats.effect.{Async, Concurrent, IO, Resource}
import cats.implicits._
import com.directbooks.core.{Accounts, LiveAccount, LiveTransaction, Transactions}
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor

final class Core[F[_]](
    accounts: Accounts[F],
    transactions: Transactions[F],
    xa: Transactor[F]
) {
  def accountsAlgebra     = accounts
  def transactionsAlgebra = transactions
  def transactor          = xa
}

object Core {
  val postgresResource: Resource[IO, HikariTransactor[IO]] = for {
    ec <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:bank",
      "docker",
      "docker",
      ec
    )
  } yield xa

  def apply: Resource[IO, Core[IO]] = {
    postgresResource.evalMap { xa =>
      for {
        acct  <- LiveAccount(xa)
        trans <- LiveTransaction(xa)
      } yield new Core(acct, trans, xa)
    }
  }
}
