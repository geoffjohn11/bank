CREATE DATABASE bank;
\c bank;

CREATE TABLE accounts(
  id serial,
  balance NUMERIC NOT NULL DEFAULT 0
 );

 CREATE TABLE transactions(
   id serial,
   accountId numeric,
   amount numeric,
   transfer text,
   status text,
   date numeric
 );

 ALTER TABLE accounts
 ADD CONSTRAINT pk_accounts PRIMARY KEY (id);

ALTER TABLE transactions
 ADD CONSTRAINT pk_transactions PRIMARY KEY (id);

 INSERT INTO accounts
 (
 id,
 balance
 ) VALUES(
 44,
 300
 );