package com.directbooks.core

import cats.effect._
import cats.implicits._
import com.directbooks.domain.Account
import doobie.ConnectionIO
import doobie.implicits._
import doobie.util._
import doobie.util.transactor.Transactor

trait Accounts[F[_]] {

  def create(balance: BigDecimal): ConnectionIO[Long]

  def find(id: Long): ConnectionIO[Option[Account]]

  def update(id: Long, balance: BigDecimal): ConnectionIO[Int]

}

class LiveAccount[F[_]: MonadCancelThrow] private (xa: Transactor[F]) extends Accounts[F] {

  override def create(balance: BigDecimal): ConnectionIO[Long] =
    sql"""
         INSERT INTO accounts
           (
           balance
           ) VALUES(
           $balance
           )
       """.update
      .withUniqueGeneratedKeys[Long]("id") // .transact(xa)

  override def find(id: Long): ConnectionIO[Option[Account]] =
    sql"""
      SELECT
        id,
        balance
       FROM accounts
       WHERE id = $id
       """.query[Account].option // .transact(xa)

  override def update(id: Long, balance: BigDecimal): ConnectionIO[Int] = {
    sql"""
         UPDATE accounts
         SET
           balance = ${balance}
         WHERE id = ${id}
       """.update.run // .transact(xa).flatMap(_ => find(id))
  }
}

object LiveAccount {
  def apply[F[_]: MonadCancelThrow](xa: Transactor[F]): F[LiveAccount[F]] =
    new LiveAccount[F](xa).pure[F]

  implicit val transactionRead: Read[Account] = Read[(Long, BigDecimal)].map {
    case (id: Long, balance: BigDecimal) =>
      Account(
        id = id,
        balance = balance
      )
  }
}
