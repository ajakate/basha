(ns basha.pages.edit-list
   (:require
    [re-frame.core :as rf]
    [basha.components.shared :refer [wrapped-string is-admin]]))

(defn edit-list []
  (let [list @(rf/subscribe [:active-list])
        is-loading @(rf/subscribe [:loading-list])
        loading-translation @(rf/subscribe [:loading-translation])]
    (when list
      [:div
       (when is-loading
         [:div.modal.is-active
          [:div.modal-background]
          [:div.modal-content>section.section
           [:div.has-text-centered.is-size-3.m-6>p.has-text-info "Loading List..."]
           [:progress.progress.is-info]]])
       [:div.px-6>div.basha-panel.mx-auto.background-semi-faded.p-5
        [:div.is-flex.is-justify-content-space-between
         [:div.is-size-3.bold.mb-3 (:name list)]
         (when (is-admin)
           [:a.button.is-orange.bold {:on-click #(rf/dispatch [:open-users-modal])} "Generate Sharing Link"])]
        [:div "Owned by " [:span.bold (:creator list)]]
        [:div.table-container.m-4.my-6>table.table.is-fullwidth
         [:thead.has-background-blue
          [:tr.has-text-white
           [:th.has-text-white "Phrase ID"]
           [:th.has-text-white "Translator"]
           [:th.has-text-white "Original Phrase"]
           [:th.has-text-white "Translated Phrase"]
           [:th.has-text-white "Audio"]
           [:th]]]
         [:tbody
          (for [s (:translations list)]
            ^{:key (:id s)}
            [:tr
             [:td (:list_index s)]
             [:td (:translator s)]
             [:td [wrapped-string (:source_text s)]]
             [:td
              [:div
               [wrapped-string (:target_text s)]
               [wrapped-string (:target_text_roman s)]]]
             [:td (if (:has_audio s)
                    [:span.icon.has-text-success>i.fa.fa-check]
                    [:span.icon.has-text-danger>i.fa.fa-ban])]
             [:td
              [:div.is-flex.is-justify-content-center
               [:a.button.is-orange.is-inverted.px-2
                (let [init {:on-click #(rf/dispatch [:fetch-translation (:id s)])}]
                  (if loading-translation
                    (if (= (:id s) (last loading-translation))
                      (assoc init :class :is-loading)
                      (assoc init :disabled :disabled))
                    init))
                "Edit"]
               [:p.centered-text "|"]
               [:a.button.is-orange.is-inverted.px-2
                (let [init {:on-click #(rf/dispatch [:set-delete-translation-id s])}]
                  (if loading-translation
                    (if (= (:id s) (last loading-translation)) ;; TODO: need this ?
                      (assoc init :class :is-loading)
                      (assoc init :disabled :disabled))
                    init))
                "Delete"]]]])]]]])))
