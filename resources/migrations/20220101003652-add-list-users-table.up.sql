CREATE TABLE list_users
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 user_id uuid not null,
 list_id uuid not null,
 created_at timestamp DEFAULT NOW() NOT NULL,
 PRIMARY KEY(id)
);
