(ns basha.modals.download
  (:require
   [re-frame.core :as rf]))

(defn download-modal []
  (let [is-active @(rf/subscribe [:is-downloading])
        errors @(rf/subscribe [:download-error])]
    (when is-active
      [:div.modal
       {:class (if is-active "is-active" nil)}
       [:div.modal-background]
       [:div.model-content>div.card.has-background-white.has-text-centered.p-6
        [:div.is-flex.is-justify-content-center>img.my-3.image.is-96x96 {:src "img/download_icon.png"}]
        [:p.has-text-orange.is-size-3.mb-4 "Downloading Your Deck..."]
        (if errors
          [:div
           [:p.has-text-danger.is-italic.my-4 errors]
           [:button.button.p-2.m-1
            {:on-click #(rf/dispatch [:kill-download-modal])}
            "Cancel"]]
          [:div
           [:span "Please stay on this page until we're done."]
           [:progress.progress.is-orange.is-large.my-4]])]])))
