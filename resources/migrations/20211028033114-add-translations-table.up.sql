CREATE TABLE translations
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 source_text VARCHAR(300) not null,
 target_text VARCHAR(300),
 target_text_roman VARCHAR(300),
 audio_link VARCHAR(300),
 list_id uuid not null,
 translator_id uuid,
 PRIMARY KEY(id)
);
