(ns basha.layout.footer
  (:require
   [basha.components.shared :refer [logo]]))

;; TODOO: fix links
(defn footer []
  [:div.foot-container.has-text-blue
   [:div [:div.mb-6 [logo]] [:p "©2022 Basha Lang"]]
   [:div]
   [:div.foot-column
    [:p.bold "About"]
    [:div.mt-1>a.link {:href "po"} "Mission and Team"]]
   [:div.foot-column
    [:p.bold "Resources"]
    [:div.mt-1>a.link {:href "po"} "SRS For Language Learning"]
    [:div.mt-1>a.link {:href "po"} "Community Decks"]]
   [:div.foot-column
    [:p.bold "Support"]
    [:div.mt-1>a.link {:href "po"} "Configuration Guide"]
    [:div.mt-1>a.link {:href "po"} "Contact Us"]]
   [:i.fa.fa-linkedin-square]
   [:div]])
