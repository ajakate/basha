CREATE TABLE translations
(id uuid DEFAULT uuid_generate_v4() NOT NULL,
 source_text VARCHAR(300) not null,
 target_text VARCHAR(300),
 target_text_roman VARCHAR(300),
 list_id uuid not null,
 translator_id uuid,
 created_at timestamp DEFAULT NOW() NOT NULL,
 list_index integer,
 audio bytea,
 PRIMARY KEY(id)
);
--;;
CREATE UNIQUE INDEX t_id ON translations(id);
--;;
CREATE INDEX t_list_id ON translations(list_id);
--;;
CREATE INDEX t_translator_id ON translations(translator_id);
--;;
CREATE INDEX t_created_at ON translations(created_at);
--;;
CREATE INDEX t_list_index ON translations(list_index);
