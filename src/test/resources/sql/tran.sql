
 CREATE TABLE transactions(
   id serial,
   accountId numeric,
   amount numeric,
   transfer text,
   status text,
   date numeric
 );

 CREATE TABLE accounts(
   id serial,
   balance NUMERIC NOT NULL DEFAULT 0
  );

ALTER TABLE transactions
ADD CONSTRAINT pk_transactions PRIMARY KEY (id);

ALTER TABLE accounts
ADD CONSTRAINT pk_accounts PRIMARY KEY (id);

INSERT INTO accounts
(
id,
balance
) VALUES(
5,
300
);