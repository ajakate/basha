(ns basha.util
  (:require
   [clojure.string :as str]
   [hashids.core :as h]))

(defn split-chunks [str-orig chunks-orig]
  ((fn [str chunks result]
     (if-let [len (first chunks)] (recur
                                   (subs str len)
                                   (rest chunks)
                                   (conj result (subs str 0 len)))
             result))
   str-orig chunks-orig []))

(def hashids-opts {:salt "TODOO: change this?"})

(defn encode-uuid [uuid]
  (let [hexval (clojure.string/replace uuid "-" "")]
    (h/encode-hex hashids-opts hexval)))

(defn decode-uuid [encoded]
  (let [raw (h/decode-hex hashids-opts encoded)
        full (str/join raw)
        split (split-chunks full [8 4 4 4 12])]
    (str/join "-" split)))
