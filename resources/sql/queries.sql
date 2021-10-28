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

-- :name create-list!* :! :n
-- :doc creates a list record
INSERT INTO lists
(name, user_id, target_language, source_language)
VALUES (:name, :user_id, :target_language, :source_language)

-- :name create-sentence!* :<! :1
-- :doc creates a sentence record
INSERT INTO sentences
(text, 
creator_id,
 --~ (if (seq (:text_roman params)) "text_roman," nil)
  language)
VALUES (:text,
 :creator_id,
--~ (if (seq (:text_roman params)) ":text_roman," nil)
  :language)
RETURNING *;
