(ns basha.layout.footer
  (:require
   [basha.components.shared :refer [logo]]))

(defn footer []
  [:div.foot-container.has-text-blue.columns.is-8
   [:div.column [:div [logo]] [:p.mt-6.is-hidden-mobile "©2023 Basha Lang"]]
   [:div.foot-column.column
    [:p.bold "About"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/mission.html" :target "_blank"} "Mission and Team"]]
   [:div.foot-column.column
    [:p.bold "Resources"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/methodology.html" :target "_blank"} "SRS For Language Learning"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/resources.html#sentence_lists" :target "_blank"} "Sentence Lists"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/resources.html#community_decks" :target "_blank"} "Community Decks"]]
   [:div.foot-column.column
    [:p.bold "Support"]
    [:div.mt-1>a.link {:href "https://www.bashalang.org/guide.html" :target "_blank"} "User Guide"]
    [:div.mt-1>a.link {:href "mailto:contact@bashalang.org"} "Contact Us"]]
   [:a.column {:href "https://github.com/ajakate/basha" :target "_blank"}
    [:i.fa.fa-github]]
   [:p.column.is-hidden-tablet "©2023 Basha Lang"]])
