(ns basha.layout.banner
  (:require
   [re-frame.core :as rf]
   [basha.events]))

;; TODOO: success banner on archive
(defn db-warning []
  (let [info @(rf/subscribe [:info])]
    [:div.notification.is-danger.is-light
     [:button.delete
      {:on-click #(rf/dispatch [:banner-info nil])}]
     [:span (str "Your Basha instance will be terminated in " (- 90 (:db-uptime-days info)) " days. ")]
     [:span "Please contact " (:admin info) " or "
      [:a {:href "/#/backup"} "click here"]
      " to do a backup and restore."]]))

(defn banner []
  (let [banner-info @(rf/subscribe [:banner-info])]
    (when (= banner-info :db-warning)
      [db-warning])))
