-- START:users
-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
INSERT INTO users
(username, password)
VALUES (:username, :password)

-- :name update-user! :! :n
-- :doc updates an existing user record
UPDATE users
SET first_name = :first_name, last_name = :last_name, email = :email
WHERE id = :id

-- :name get-user :? :1
-- :doc retrieves a user record given the id
SELECT * FROM users
WHERE id = :id

-- :name get-user-for-login :? :1
-- :doc retrieves a user record given the id
SELECT username,password,id FROM users
WHERE username = :username

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id

-- :name create-list!* :! :1
-- :doc creates a list record
INSERT INTO lists
(name, user_id, target_language, source_language)
VALUES (:name, :user_id, :target_language, :source_language)
RETURNING id;

-- :name create-sentence!* :<! :1
-- :doc creates a sentence record
INSERT INTO sentences
(
   --~ (if (seq (:text params)) "text," nil)
creator_id,
 --~ (if (seq (:text_roman params)) "text_roman," nil)
  language)
VALUES (
   --~ (if (seq (:text params)) ":text," nil)
 :creator_id,
--~ (if (seq (:text_roman params)) ":text_roman," nil)
  :language)
RETURNING *;

-- :name create-list-item!* :! :1
-- :doc Insert list item
insert into list_items
(sentence_id,
list_id)
VALUES (:sentence_id :list_id)

-- :name create-translation!* :<! :1
-- :doc creates a translation record
INSERT INTO translations
(source_id, 
target_id)
VALUES (:source_id,
 :target_id)
RETURNING *;

-- :name update-sentence-audio!* :<! :1
-- :doc adds 
INSERT INTO sentences
(audio_link)
VALUES (:audio-link)
WHERE id = :id
RETURNING *;

-- :name get-sentence-by-id :? :1
-- :doc retrieves a sentence record given the id
SELECT * FROM sentences
WHERE id = :id
