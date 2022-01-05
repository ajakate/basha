CREATE TABLE lists
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 user_id uuid not null,
 name VARCHAR(300) not null,
 source_language VARCHAR(300) not null,
 target_language VARCHAR(300),
 created_at timestamp DEFAULT NOW() NOT NULL,
 PRIMARY KEY(id)
);
--;;
CREATE UNIQUE INDEX l_id ON lists(id);
--;;
CREATE INDEX l_user_id ON lists(user_id);
--;;
CREATE INDEX l_created_at ON lists(created_at);
