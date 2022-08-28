ALTER TABLE lists
    ADD COLUMN has_latin_script BOOLEAN;
--;;
UPDATE lists
    SET has_latin_script = false;
--;;
ALTER TABLE lists ALTER COLUMN has_latin_script SET NOT NULL;
--;;
ALTER TABLE lists ALTER COLUMN has_latin_script SET DEFAULT FALSE;
