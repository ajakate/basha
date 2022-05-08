-- :name update-translation :! :1
-- :doc updates translation
update translations
set
--~ (if (seq (:source_text params)) "source_text = :source_text," nil)
--~ (if (seq (:target_text params)) "target_text = :target_text," "target_text = null,")
--~ (if (seq (:target_text_roman params)) "target_text_roman = :target_text_roman," "target_text_roman = null,")
--~ (if (seq (:audio params)) "audio = :audio," nil)
translator_id = :translator_id
where id = :id
RETURNING *;
