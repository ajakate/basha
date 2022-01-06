(ns basha.languages)

(def supported-languages {"English" {:has_latin_script true}
                "Marathi" {:has_latin_script false}
                "Spanish" {:has_latin_script true}})

(def language-list (keys supported-languages))

(defn has-latin-script [lang]
  (:has_latin_script (get supported-languages lang)))
