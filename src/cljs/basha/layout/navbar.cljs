(ns basha.layout.navbar
  (:require
   [re-frame.core :as rf]))

;; TODOO: fix links
(defn navbar []
  (let [user @(rf/subscribe [:user])
        user-exists (seq user)]
    [:nav
     [:div.nav-container
      [:div.logo
       [:a {:href "https://www.bashalang.org" :target "_blank"}
        [:img {:src "/img/logo.png" :alt "logo"}]
        [:p "Basha"]]]
      [:div]
      (if user-exists
        [:a.link.centered-text.color-turq.bold {:href "/#/"} "Dashboard"]
        [:div])
      [:a.link.centered-text.color-turq.bold {:href "https://www.bashalang.org" :target "_blank"} "Resources"]
      (if user-exists
        [:a.link.centered-text.color-turq.bold {:on-click #(rf/dispatch [:logout])} "Logout"]
        [:a.link.centered-text.color-turq.bold {:on-click #(rf/dispatch [:open-login-modal])} "Login"])]]))
