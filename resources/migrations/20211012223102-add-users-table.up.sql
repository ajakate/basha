CREATE TABLE users
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 username VARCHAR(30) not null,
 password VARCHAR(300) not null,
 created_at timestamp DEFAULT NOW() NOT NULL,
 PRIMARY KEY(id)
);
