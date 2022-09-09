(ns basha.layout.banner
  (:require
   [re-frame.core :as rf]
   [basha.events]))

(defn common-notification [color-class children]
  [:div.notification.is-light
   {:class color-class}
   [:button.delete
    {:on-click #(rf/dispatch [:banner-info nil])}]
   children])

(defn db-warning []
  [common-notification
   "is-danger"
   (let [info @(rf/subscribe [:info])]
     [:div [:span (str "Your Basha instance will be terminated in " (- 90 (:db-uptime-days info)) " days. ")]
      [:span "Please contact " (:admin info) " or "
       [:a {:href "/#/backup"} "click here"]
       " to do a backup and restore."]])])

(defn restore-success []
  [common-notification
   "is-success"
   [:span (str "Congratulations! You've restored your Basha instance. Feel free to login with your old account info.")]])

(defn banner []
  (let [banner-info @(rf/subscribe [:banner-info])]
    (case banner-info
      :db-warning [db-warning]
      :restore-success [restore-success]
      nil)))
