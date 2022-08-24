(ns basha.components.shared)

;; TODOO: fix formatting bold
(defn logo []
  [:div.logo
   [:a {:href "https://www.bashalang.org" :target "_blank"}
    [:img {:src "/img/logo.png" :alt "logo"}]
    [:p "Basha"]]])
