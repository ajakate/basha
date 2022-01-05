CREATE TABLE list_users
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 user_id uuid not null,
 list_id uuid not null,
 created_at timestamp DEFAULT NOW() NOT NULL,
 PRIMARY KEY(id)
);
--;;
CREATE INDEX lu_list_id ON list_users(list_id);
--;;
CREATE INDEX lu_user_id ON list_users(user_id);
