(ns basha.layout.navbar
  (:require
   [re-frame.core :as rf]
   [basha.components.shared :refer [logo]]))

;; TODOO: fix links
(defn navbar []
  (let [user @(rf/subscribe [:user])
        user-exists (seq user)]
    [:nav
     [:div.nav-container
      [logo]
      [:div]
      [:div]
      [:div]
      (if user-exists
        [:a.link.centered-text.color-turq.bold {:href "/#/"} "Dashboard"]
        [:div])
      [:a.link.centered-text.color-turq.bold {:href "https://www.bashalang.org" :target "_blank"} "Resources"]
      (if user-exists
        [:a.link.centered-text.color-turq.bold {:on-click #(rf/dispatch [:logout])} "Logout"]
        [:a.link.centered-text.color-turq.bold {:href "/#/login"} "Login"])]]))
