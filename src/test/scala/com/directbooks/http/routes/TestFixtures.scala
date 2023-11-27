package com.directbooks.http.routes


import com.directbooks.domain.{Account, Transaction, TransactionInfo}

trait TestFixtures {

  val transactFixture =  Transaction(1L, 123L, BigDecimal("500"), "withdrawal", "success", 123456L)

  //TODO might also need bigDecimal validation
  val withdrawalFixture = TransactionInfo(5L, BigDecimal("500"), "withdrawal")

  val accountFixture = Account(123L, BigDecimal(600))


}
