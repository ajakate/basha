CREATE TABLE list_items
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 list_id uuid not null,
 sentence_id uuid not null,
 PRIMARY KEY(id)
);
