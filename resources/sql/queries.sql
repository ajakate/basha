-- START:users
-- :name create-user!* :! :n
-- :doc creates a new user with the provided login and hashed password
INSERT INTO users
(username, pass)
VALUES (:username, :pass)

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
SELECT username,pass FROM users
WHERE username = :username

-- :name delete-user! :! :n
-- :doc deletes a user record given the id
DELETE FROM users
WHERE id = :id
