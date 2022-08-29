(ns basha.modals.share
  (:require
   [re-frame.core :as rf]
   [basha.helpers :refer [copy-to-clipboard]]))

(defn share-link [code]
  (let [origin (.-origin js/window.location)]
    (str origin "/#/share/" code)))

(defn share-modal []
  (let [active @(rf/subscribe [:users-modal-visible])
        list @(rf/subscribe [:active-list])]
    (when active
      [:div.modal
       {:class (if active "is-active" nil)}
       [:div.modal-background]
       [:div.model-content>div.card.has-background-white.has-text-centered
        (let [link (share-link (:share_code list))]
          [:div.p-5
           [:div.is-flex.is-justify-content-center>img.my-3.image.is-96x96 {:src "img/link_icon.png"}]
           [:p.py-4 "Anyone with this link will be able to view and edit this deck"]
           [:div.is-flex
            [:input.input
             {:type "text"
              :value link}]
            [:button.button.has-text-orange.ml-2
             {:on-click #(copy-to-clipboard link)}
             [:i.fa.fa-clone]]]
           [:button.button.mt-5
            {:on-click #(rf/dispatch [:close-users-modal])}
            "Close"]])
        [:button.modal-close.is-large
         {:aria-label "close" :on-click #(rf/dispatch [:close-users-modal])} "close"]]])))
