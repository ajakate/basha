CREATE TABLE lists
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 user_id uuid not null references users on delete cascade,
 name VARCHAR(300) not null,
 source_language VARCHAR(300) not null,
 target_language VARCHAR(300),
 created_at timestamp DEFAULT NOW() NOT NULL,
 PRIMARY KEY(id),
 CONSTRAINT fk_user
      FOREIGN KEY(user_id) 
	  REFERENCES users(id)
);
