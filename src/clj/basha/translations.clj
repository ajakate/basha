(ns basha.translations
  (:require
   [basha.db.core :as db]
   [clojure.java.io :as io]
   [honey.sql :as sql]))

(defn file2bytes [path]
  (with-open [in (io/input-stream path)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn fetch [id]
  (db/execute-one
   (sql/format
    {:select :*
     :from :translations
     :where [:= :id (java.util.UUID/fromString id)]})))

(defn delete-translation [id]
  (db/execute-one
   (sql/format
    {:delete-from :translations
     :where [:= :id (java.util.UUID/fromString id)]})))

(defn update-translation [id user-id params]
  (let [audio (-> params :audio :tempfile)
        params (dissoc params :audio)
        full (assoc params
                    :translator_id (java.util.UUID/fromString user-id)
                    :audio (if audio (file2bytes audio) nil))
        filtered (into {} (filter (comp some? val) full))]
    (db/execute-one
     (sql/format
      {:update :translations
       :set filtered
       :where [:= :id (java.util.UUID/fromString id)]}))))

(defn delete-audio [id]
  (db/execute-one
   (sql/format
    {:update :translations
     :set {:audio nil}
     :where [:= :id (java.util.UUID/fromString id)]})))
