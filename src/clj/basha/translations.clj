(ns basha.translations
  (:require
   [basha.db.core :as db]
   [clojure.java.io :as io]))

(defn file2bytes [path]
  (with-open [in (io/input-stream path)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn fetch [id]
  (db/get-translation {:id (java.util.UUID/fromString id)}))

(defn update [id user-id params]
  (let [audio (-> params :audio :tempfile)
        params (dissoc params :audio)]
    (db/update-translation (assoc params
                                  :id (java.util.UUID/fromString id)
                                  :translator_id (java.util.UUID/fromString user-id)
                                  :audio (if audio (file2bytes audio) nil)))))

(defn delete-audio [id]
  (db/delete-audio-for-translation {:id (java.util.UUID/fromString id)}))
