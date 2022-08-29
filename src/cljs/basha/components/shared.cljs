(ns basha.components.shared
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]))

;; TODOO: fix formatting bold
(defn logo []
  [:div.logo
   [:a {:href "https://www.bashalang.org" :target "_blank"}
    [:img {:src "/img/logo.png" :alt "logo"}]
    [:p "Basha"]]])

(defn format-string [st]
  (let [words (map #(str % \space) (string/split st #" "))]
    (partition 10 10 nil words)))

(defn wrapped-string [st]
  [:div
   (for [l (format-string st)]
     ^{:key (str l)}
     [:p l])])

(defn is-admin []
  (let [user @(rf/subscribe [:user])
        info  @(rf/subscribe [:info])]
    (= (:admin info) (:username user))))
