CREATE TABLE sentences
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 creator_id uuid not null references users,
 editor_id uuid references users,
 text VARCHAR(300) not null,
 text_roman VARCHAR(300),
 audio_link VARCHAR(300),
 language VARCHAR(300) not null,
 PRIMARY KEY(id)
);
