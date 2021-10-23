CREATE TABLE users
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 username VARCHAR(30) not null,
 password VARCHAR(300) not null,
 PRIMARY KEY(id)
);
