package com.directbooks.domain

case class AccountInfo(startingBalance: BigDecimal)
case class Account(id: Long, balance: BigDecimal)

case class TransactionInfo(accountId: Long, amount: BigDecimal, description: String)
case class Transaction(
    id: Long,
    accountId: Long,
    amount: BigDecimal,
    transfer: String,
    status: String,
    date: Long
)
