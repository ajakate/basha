-- :name get-list :? :*
-- :doc fetches list
select *, t.id as translation_id,
l.id as list_id,
u.username as creator,
(select username from users where t.translator_id = users.id) translator,
(select string_agg(uu.username, ',')
 from users uu
 join list_users lluu on lluu.user_id=uu.id
 where lluu.list_id = :id) users,
 (t.audio is not null) has_audio
 from translations t
join lists l on l.id=t.list_id
join users u on l.user_id=u.id
where l.id = :id
order by target_text_roman is null desc, target_text is null desc, t.list_index asc;

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
