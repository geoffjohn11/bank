package com.directbooks.core

import cats._
import cats.effect._
import cats.implicits._
import com.directbooks.domain.Transaction
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util._
import doobie.util.transactor.Transactor

import java.util.UUID

trait Transactions[F[_]] {
  def create(info: Transaction): ConnectionIO[Long]

  def findByAccountId(accountId: Long): ConnectionIO[List[Transaction]]

  def findByTransactionId(id: Long): ConnectionIO[List[Transaction]]
}

class LiveTransaction[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Transactions[F] {

  override def create(info: Transaction): ConnectionIO[Long] =
    sql"""
         INSERT INTO transactions
           (
             accountId,
             amount,
             transfer,
             status,
             date) VALUES(
             ${info.accountId},
             ${info.amount},
             ${info.transfer},
             ${info.status},
             ${System.currentTimeMillis()}
           )
       """.update
      .withUniqueGeneratedKeys[Long]("id")

  override def findByAccountId(accountId: Long): ConnectionIO[List[Transaction]] =
    sql"""
      SELECT
        id,
        accountId,
        amount,
        transfer,
        status,
        date
       FROM transactions
       WHERE accountId = $accountId
       """.query[Transaction].to[List]

  override def findByTransactionId(id: Long): ConnectionIO[List[Transaction]] =
    sql"""
    SELECT
      id,
      accountId,
      amount,
      transfer,
      status,
      date
     FROM transactions
     WHERE id = $id
     """.query[Transaction].to[List]
}

object LiveTransaction {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveTransaction[F]] =
    new LiveTransaction[F](xa).pure[F]

  implicit val transactionRead: Read[Transaction] =
    Read[(Long, Long, BigDecimal, String, String, Long)].map {
      case (
            id: Long,
            accountId: Long,
            amount: BigDecimal,
            transfer: String,
            status: String,
            date: Long
          ) =>
        Transaction(
          id = id,
          accountId = accountId,
          amount = amount,
          transfer = transfer,
          status = status,
          date = date
        )
    }
}
