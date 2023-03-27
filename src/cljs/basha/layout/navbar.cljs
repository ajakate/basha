(ns basha.layout.navbar
  (:require
   [re-frame.core :as rf]
   [basha.components.shared :refer [logo]]
   [reagent.core :as r]))

;; TODON: fix logo vert alignment
(defn navbar []
  (let [user @(rf/subscribe [:user])
        user-exists (seq user)]
    (r/with-let [expanded? (r/atom false)]
      [:nav.navbar
       [:div.navbar-brand.mt-4.mx-4
        [logo]
        [:a.navbar-burger
         {:role "button"
          :aria-label "menu"
          :aria-expanded false
          :data-target "poo"
          :on-click #(swap! expanded? not) :class (when @expanded? :is-active)}
         [:span {:aria-hidden true}]
         [:span {:aria-hidden true}]
         [:span {:aria-hidden true}]]]
       [:div#poo.navbar-menu
        {:class (when @expanded? :is-active)}
        [:div.navbar-start
         (if user-exists
           [:a.navbar-item.link.centered-text.color-turq.bold.ml-4 {:href "/#/"} "Dashboard"]
           [:div])
         [:a.navbar-item.link.centered-text.color-turq.bold.ml-4 {:href "https://www.bashalang.org/resources.html" :target "_blank"} "Resources"]
         (if user-exists
           [:a.navbar-item.link.centered-text.color-turq.bold.ml-4 {:on-click #(rf/dispatch [:logout])} "Logout"]
           [:a.navbar-item.link.centered-text.color-turq.bold.ml-4 {:href "/#/login"} "Login"])]]])))
