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
       [:div.model-content>div.card
        [:header.card-header>p.card-header-title "Confirm Delete"]
        [:div.card-content.has-text-centered
         [:span "Are you sure you want to delete "]
         [:span.has-text-weight-bold name]
         [:span "?"]
         [:p "This action is irreversible..."]
         [:div.columns.mt-4
          [:div.column.control>button.button.is-danger
           {:on-click #(rf/dispatch [delete-func id])} "Delete"]
          [:div.column.control>button.button
           {:on-click #(rf/dispatch [clear-delete-id])} "Cancel"]]]]])))
