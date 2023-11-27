# Bank App

App that supports banking withdrawal and deposit operations to an account.  The implementation uses the Typelevel stack, including
Cats, CatsEffect, Http4s, and doobie

The app supports two operations against an account:
```withdrawal``` and ```deposit```

## Description
The application itself is stateless, all state is maintained in a postgres database.  The ACID compliant database provides strong
guarantees about the processing of transactions.  The process of checking if funds are available and removing or adding to the 
balance of an account happens as a single database transaction.

### Tested with SDK versions
* sbt 1.9.7
* Java 11.0.18
* Scala 2.13.12
* Docker version 24.0.7

### Running the App

* clone repo
* cd into the ```bank``` directory
* run command ```docker compose up``` to start postgres, init.sql will insert a row into the account table with id ```44```
* command ```sbt run``` executes the main app in com.directbooks.Application and starts an Ember http server
* to see the pre-created account execute http GET request http://localhost:8080/directbooks/account/44 
* expected response payload:
```json
{
	"id": 44,
	"balance": 300
}
```
* a transaction to withdraw 200 can be executed against account 44 with the http POST
http://localhost:8080/directbooks/transaction with a body payload
```json
{
	"accountId": 44,
	"amount": 200,
	"description": "withdrawal"
}
```
* response payload:
```json
[
	{
		"id": 1,
		"accountId": 44,
		"amount": 200,
		"transfer": "withdrawal",
		"status": "success",
		"date": 1701049151121
	}
]
```
* Another transaction to overdraw the account can be executed with a POST to http://localhost:8080/directbooks/transaction 
with body payload
```json
{
	"accountId": 44,
	"amount": 301,
	"description": "withdrawal"
}
```

* The Response is a 422
* transaction history can be seen with a GET request http://localhost:8080/directbooks/transaction/history/44 with response
```json
[
	{
		"id": 1,
		"accountId": 44,
		"amount": 200,
		"transfer": "withdrawal",
		"status": "success",
		"date": 1701049151121
	},
	{
		"id": 2,
		"accountId": 44,
		"amount": 301,
		"transfer": "withdrawal",
		"status": "failed",
		"date": 1701049571660
	}
]
```

* command ```sbt test``` executes the tests, which spin up a PostgreSQLContainer testing container
