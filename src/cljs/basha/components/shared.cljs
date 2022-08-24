(ns basha.components.shared)

(defn logo []
  [:div.logo
   [:a {:href "https://www.bashalang.org" :target "_blank"}
    [:img {:src "/img/logo.png" :alt "logo"}]
    [:p "Basha"]]])
