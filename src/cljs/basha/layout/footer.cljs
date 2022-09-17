(ns basha.layout.footer
  (:require
   [basha.components.shared :refer [logo]]))

(defn footer []
  [:div.foot-container.has-text-blue
   [:div [:div.mb-6 [logo]] [:p "©2022 Basha Lang"]]
   [:div]
   [:div.foot-column
    [:p.bold "About"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/mission.html" :target "_blank"} "Mission and Team"]]
   [:div.foot-column
    [:p.bold "Resources"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/methodology.html" :target "_blank"} "SRS For Language Learning"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/resources.html#sentence_lists" :target "_blank"} "Sentence Lists"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/resources.html#community_decks" :target "_blank"} "Community Decks"]]
   [:div.foot-column
    [:p.bold "Support"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/guide.html" :target "_blank"} "User Guide"]
    [:div.mt-1>a.link {:href "mailto:contact@bashalang.org"} "Contact Us"]]
   [:a {:href "https://github.com/ajakate/basha" :target "_blank"}
    [:i.fa.fa-github]]
   [:div]])
