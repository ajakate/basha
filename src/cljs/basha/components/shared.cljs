(ns basha.components.shared
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]))

(defn logo []
  [:div.logo.m-3
   [:a.columns.is-mobile {:href "https://www.bashalang.org" :target "_blank"}
    [:img.is-inline {:src "/img/logo.png" :alt "logo"}]
    [:p.is-inline "Basha"]]])

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

(defn loading-screen [sub]
  (let [is-loading @(rf/subscribe [sub])]
    (when is-loading
      [:div.modal-background.loading-screen.flex.centered-text
       [:div [:p.mb-5.is-size-3 "Loading..."]
        [:div.flex.centered-text>div.is-size-1.loader-mixin]]])))
