-- :name create-list!* :! :1
-- :doc creates a list record
INSERT INTO lists
(name, user_id, target_language, source_language)
VALUES (:name, :user_id, :target_language, :source_language)
RETURNING id;

-- :name get-list-summary :? :*
-- :doc fetches list summary for user
select l.id,
l.name,
l.source_language,
l.target_language,
(select u.username from users u where u.id=l.user_id) creator,
count(t.id) list_count,
count(t.id) filter (where (audio is null) or (target_text_roman is null)) open_count,
(select string_agg(uu.username, ',')
   from users uu
   join list_users lluu on lluu.user_id=uu.id
   where lluu.list_id=l.id) users
from lists l
join translations t on t.list_id=l.id
full outer join list_users li on li.list_id=l.id
where l.user_id=:id or li.user_id=:id
group by l.id
order by
(count(t.id) filter (where (audio is null) or (target_text_roman is null)) = 0) asc,
l.created_at asc;

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

-- :name get-translation :? :1
-- :doc fetches translation
select * from translations t
where t.id = :id;

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

-- :name get-users-by-username :? :*
-- :doc retrieves users by username
SELECT id,username FROM users u
WHERE u.username in (:v*:users)

-- :name delete-list-users :! :n
-- :doc retrieves users by username
delete from list_users
where list_id = :list_id;

-- :name create-list-users :! :n
-- :doc retrieves users by username
insert into list_users
(user_id, list_id)
values :t*:users;

-- :name delete-audio-for-translation :! :1
-- :doc updates translation removes audio
update translations
set audio = null
where id = :id
RETURNING *;
