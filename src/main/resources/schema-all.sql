DROP TABLE CUSTOMER IF EXISTS;

CREATE TABLE CUSTOMER  (
    customer_id BIGINT IDENTITY NOT NULL PRIMARY KEY,
    first_name VARCHAR(20),
    last_name VARCHAR(20),
    account_no VARCHAR(20)
   );