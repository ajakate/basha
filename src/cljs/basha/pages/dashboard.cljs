(ns basha.pages.dashboard
  (:require 
   [re-frame.core :as rf]
   [basha.components.shared :refer [is-admin loading-screen]]))

(defn no-lists []
  [:div.has-text-centered
   [:img.py-6 {:src "/img/empty_state_graphic.png"}]
   [:p.my-3.mx-6.px-6 "You haven't uploaded any decks yet. You can either upload your own list of phrases as a text file or download an example from our community library to re-upload."]
   [:a.link>div.mt-1 "Upload a Deck →"]
   [:a.link>div.mt-1 "Download a Community Deck →"]
   [:a.link>div.mt-1 "Get Help →"]])

(defn list-summary-table [lists]
  [:div.table-container.m-4.mb-6>table.table.is-fullwidth.is-hidden-mobile
   [:thead.has-background-blue
    [:tr.has-text-white
     [:th.has-text-white "Language"]
     [:th.has-text-white "Name"]
     [:th.has-text-white "Shares"]
     [:th.has-text-white "Progress"]
     [:th]]]
   [:tbody
    (for [list lists]
      ^{:key (:id list)}
      (let [done_count (- (:list_count list) (:open_count list))
            percentage (* 100 (/ done_count (:list_count list)))]
        [:tr
         [:td (:target_language list)]
         [:td (:name list)]
         [:td (:users list)]
         [:td
          [:progress.progress.is-green.my-3 {:value percentage :max "100"}]
          [:p (str done_count "/" (:list_count list) " items translated")]]
         [:td
          [:div.is-flex.is-justify-content-center
           [:a.link.has-text-orange.px-2
            {:href (str "/#/lists/edit/" (:id list))}
            "Edit"]
           [:p "|"]
           [:a.link.has-text-orange.px-2
            {:on-click #(rf/dispatch [:set-downloading-deck (:id list) (:name list)])}
            "Download"]
           (when (is-admin)
             [:<>
              [:p "|"]
              [:a.link.has-text-orange.px-2
               {:on-click #(rf/dispatch [:set-delete-list-id list])}
               "Delete"]])]]]))]])

(defn list-summary-table-mobile [lists]
  [:div.table-container>table.table.is-fullwidth.is-hidden-tablet
   [:thead.has-background-blue
    [:tr.has-text-white
     [:th.has-text-white "List Info"] 
     [:th.has-text-white "Progress"]
     [:th]]]
   [:tbody
    (for [list lists]
      ^{:key (:id list)}
      (let [done_count (- (:list_count list) (:open_count list))
            percentage (* 100 (/ done_count (:list_count list)))]
        [:tr
         [:td.columns.is-gapless
          [:div.column.mb-1.bold (:name list)]
          [:div.column.mb-4.is-italic (:target_language list)]
          [:div.column
           [:div
            [:p.bold "Shared with:"]
            [:p (:users list)]]]]
         [:td
          [:progress.progress.is-green.my-3 {:value percentage :max "100"}]
          [:p (str done_count "/" (:list_count list) " items translated")]]
         [:td
          [:div.columns.is-gapless
           [:a.link.has-text-orange.px-2.my-4.column
            {:href (str "/#/lists/edit/" (:id list))}
            "Edit"]
           [:a.link.has-text-orange.px-2.my-4.column
            {:on-click #(rf/dispatch [:set-downloading-deck (:id list) (:name list)])}
            "Download"]
           (when (is-admin)
             [:a.link.has-text-orange.px-2.my-4.column
              {:on-click #(rf/dispatch [:set-delete-list-id list])}
              "Delete"])]]]))]])

(defn dashboard-page []
  (let [lists @(rf/subscribe [:list-summary])]
    [:div.basha-panel.mx-auto.background-semi-faded
     [loading-screen :loading-list-summary]
     [:div.columns.p-4
      [:div.is-size-3.bold.mb-3.column "My Dashboard"]
      (when (is-admin)
        [:div
         [:div.column.is-justify-content-flex-end.is-flex.is-hidden-mobile
          [:a.button.is-orange.bold
           {:on-click #(rf/dispatch [:create-deck-modal-visible true])}
           "Add New Deck"]]
         [:div.column.is-justify-content-flex-start.is-flex.is-hidden-tablet
          [:a.button.is-orange.bold
           {:on-click #(rf/dispatch [:create-deck-modal-visible true])}
           "Add New Deck"]]])]
     [:div.columns.p-4>p.column.py-0 "Use this hub to upload your files, manage translations, and export decks."]
     (if (seq lists)
       [:div
        [list-summary-table lists]
        [list-summary-table-mobile lists]]
       [no-lists])]))
