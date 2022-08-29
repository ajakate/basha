(ns basha.modals.delete
  (:require
   [day8.re-frame.http-fx] ;; TODO: optimize
   [re-frame.core :as rf]
   [basha.events]))

(defn delete-modal [sub-delete-id clear-delete-id delete-func name-key]
  (let [active @(rf/subscribe [sub-delete-id])
        id (:id active)
        name (name-key active)]
    (when active
      [:div.modal
       {:class (if active "is-active" nil)}
       [:div.modal-background]
       [:div.model-content>div.card.has-background-white.has-text-centered.p-4
        [:div.is-flex.is-justify-content-center>img.my-3.image.is-96x96 {:src "img/delete_icon.png"}]
        [:h2.has-text-red.is-size-3.mb-4 "Confirm Delete"]
        [:div
         [:span "Are you sure you want to delete "]
         [:span.has-text-weight-bold name]
         [:span "?"]
         [:p "We won't be able to restore it if you change your mind."]
         [:div.flex.is-justify-content-center.mt-4
          [:button.button.m-3
           {:on-click #(rf/dispatch [clear-delete-id])} "Cancel"]
          [:button.button.m-3.is-danger
           {:on-click #(rf/dispatch [delete-func id])} "Delete"]]]]])))
