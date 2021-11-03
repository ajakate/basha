CREATE TABLE translations
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 source_id uuid not null,
 target_id uuid not null,
 PRIMARY KEY(id)
);
