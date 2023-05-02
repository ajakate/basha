(ns basha.pages.edit-list
   (:require
    [re-frame.core :as rf]
    [basha.components.shared :refer [wrapped-string is-admin loading-screen]]))

(defn list-table-desktop [list loading-translation]
  [:div.table-container.m-4.mb-6>table.table.is-fullwidth.is-hidden-mobile
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
          "Delete"]]]])]])

(defn list-table-mobile [list loading-translation]
  [:div.table-container>table.table.is-fullwidth.is-hidden-tablet
   [:thead.has-background-blue
    [:tr.has-text-white
     [:th.has-text-white "Details"]
     [:th.has-text-white "Phrase"]
     [:th]]]
   [:tbody
    (for [s (:translations list)]
      ^{:key (:id s)}
      [:tr
       [:td.columns.is-gapless
        [:div.column.mb-1  (:list_index s)]
        [:div.column.mb-4  [:span.bold "Translator: "] (:translator s)]
        [:div.column.mb-4.is-italic
         [:span "Audio? " (if (:has_audio s)
                            [:span.icon.has-text-success>i.fa.fa-check]
                            [:span.icon.has-text-danger>i.fa.fa-ban])]]]
       [:td.columns.is-gapless
        [wrapped-string (:source_text s)]
        [:div.is-italic.mt-6
         [wrapped-string (:target_text s)]
         [wrapped-string (:target_text_roman s)]]]
       [:td
        [:div.columns.is-gapless
         [:a.button.is-orange.is-inverted.px-2
          (let [init {:on-click #(rf/dispatch [:fetch-translation (:id s)])}]
            (if loading-translation
              (if (= (:id s) (last loading-translation))
                (assoc init :class :is-loading)
                (assoc init :disabled :disabled))
              init))
          "Edit"]
         [:a.button.is-orange.is-inverted.px-2
          (let [init {:on-click #(rf/dispatch [:set-delete-translation-id s])}]
            (if loading-translation
              (if (= (:id s) (last loading-translation)) ;; TODO: need this ?
                (assoc init :class :is-loading)
                (assoc init :disabled :disabled))
              init))
          "Delete"]]]])]])

(defn edit-list []
  (let [list @(rf/subscribe [:active-list])
        loading-translation @(rf/subscribe [:loading-translation])]
    [:div
     [loading-screen :loading-list]
     [:div>div.basha-panel.mx-auto.background-semi-faded
      [:div.columns.p-4
       [:div.is-size-3.bold.mb-3.column (:name list)]
       (when (is-admin)
         [:<>
          [:div.column.is-justify-content-flex-end.is-flex.is-hidden-mobile
           [:a.button.is-orange.bold {:on-click #(rf/dispatch [:open-users-modal])} "Generate Sharing Link"]]
          [:div.column.is-justify-content-flex-start.is-flex.is-hidden-tablet
           [:a.button.is-orange.bold {:on-click #(rf/dispatch [:open-users-modal])} "Generate Sharing Link"]]])]
      [:div.p-4 "Owned by " [:span.bold (:creator list)]]
      [list-table-desktop list loading-translation]
      [list-table-mobile list loading-translation]]]))
