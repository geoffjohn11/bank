package com.directbooks.http.routes

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import com.directbooks.core.{LiveAccount, LiveTransaction}
import com.directbooks.domain.Transaction
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import io.circe.generic.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl._
import org.http4s.implicits._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.testcontainers.containers.PostgreSQLContainer
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
class TransactionRoutesSpec extends AsyncFreeSpec with AsyncIOSpec with Matchers with Http4sDsl[IO] with TestFixtures {

  val postgres: Resource[IO, PostgreSQLContainer[Nothing]] = {
    val acquire = IO {
      val container: PostgreSQLContainer[Nothing] = new PostgreSQLContainer("postgres").withInitScript("sql/tran.sql")
      container.start()
      container
    }
    val release = (container: PostgreSQLContainer[Nothing]) => IO(container.stop())
    Resource.make(acquire)(release)
  }

  val transactor: Resource[IO, Transactor[IO]] = for{
    db <- postgres
    ce <- ExecutionContexts.fixedThreadPool[IO](1)
    xa <- HikariTransactor.newHikariTransactor[IO]("org.postgresql.Driver",db.getJdbcUrl(), db.getUsername(), db.getPassword(), ce)
  } yield (xa)


  implicit val logger: Logger[IO] = Slf4jLogger.getLogger[IO]


  "TransactionRoutes" - {
    "should return entity if transaction details if withdrawal amount is less or equal to than balance" in {
      transactor.use{ xa =>
        for {
          lt <- LiveTransaction[IO](xa)
          la <- LiveAccount[IO](xa)
          route = TransactionRoutes[IO](la, lt, xa)
          response <- route.routes.orNotFound.run(
            Request(method = Method.POST, uri = uri"/transaction").withEntity(withdrawalFixture.copy(amount = BigDecimal(5)))
          )
          retrieved <- response.as[List[Transaction]]
        } yield {
          response.status shouldBe Status.Created
          retrieved.map(_.status) shouldBe List("success")
          retrieved.map(_.amount) shouldBe List(5)
          retrieved.map(_.accountId) shouldBe List(5)
        }
      }
    }
    "should return unprocessable entity if withdrawal exceeds balance" in {
      transactor.use { xa =>
        for {
          lt <- LiveTransaction[IO](xa)
          la <- LiveAccount[IO](xa)
          route = TransactionRoutes[IO](la, lt, xa)
          response <- route.routes.orNotFound.run(
            Request(method = Method.POST, uri = uri"/transaction").withEntity(withdrawalFixture)
          )
          retrieved <- response.as[String]
        } yield {
          response.status shouldBe Status.UnprocessableEntity
          retrieved shouldBe "Withdrawal exceeds balance"
        }
      }
    }
    "should return account not found if account does not exist" in {
      transactor.use { xa =>
        for {
          lt <- LiveTransaction[IO](xa)
          la <- LiveAccount[IO](xa)
          route = TransactionRoutes[IO](la, lt, xa)
          response <- route.routes.orNotFound.run(
            Request(method = Method.POST, uri = uri"/transaction").withEntity(withdrawalFixture.copy(accountId = 789))
          )
          retrieved <- response.as[String]
        } yield {
          response.status shouldBe Status.NotFound
          retrieved shouldBe "Account does not exist"
        }
      }
    }
  }


}
