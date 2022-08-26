(ns basha.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [basha.ajax :as ajax]
   [basha.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string]
   [basha.modals.delete :refer [delete-modal]]
   [basha.modals.create-deck :refer [create-deck-modal]]
   [basha.layout.navbar :refer [navbar]]
   [basha.layout.footer :refer [footer]]
   [basha.pages.login :refer [login-page]]
   [basha.pages.dashboard :refer [dashboard-page]]
   [basha.pages.edit-list :refer [edit-list]]
   [basha.modals.download :refer [download-modal]]
   [basha.modals.translate :refer [translate-modal]]))

;; TODOO: DELETE THESE!!!!!
(defn format-string [st]
  (let [words (map #(str % \space) (string/split st #" "))]
    (partition 10 10 nil words)))

(defn wrapped-string [st]
  [:div
   (for [l (format-string st)]
     ^{:key (str l)}
     [:p l])])

(defn assign-modal []
  (let [is-active @(rf/subscribe [:users-modal-visible])
        list @(rf/subscribe [:active-list])
        error @(rf/subscribe [:users-error])]
    (when is-active
      (r/with-let [draft_users (r/atom (:users list))]
        [:div.modal
         {:class (if is-active "is-active" nil)}
         [:div.modal-background]
         [:div.model-content>div.card
          [:header.card-header
           [:p.card-header-title "Edit sharing"]]
          [:div.card-content
           [:div
            [:div.field
             [:label.label "Assignees"]
             [:div.control>input.input
              {:type "text"
               :placeholder "sk8hkr69"
               :on-change #(reset! draft_users (.. % -target -value))
               :value @draft_users}]]
            (when error [:p.has-text-danger.is-italic.mb-3 (str "Error: " error)])
            [:div.columns
             [:div.column.control>button.button.is-link
              {:on-click #(rf/dispatch [:edit-users {:users (string/split @draft_users #",") :list_id (:id list)}])} "Submit"]
             [:div.column.control>button.button
              {:on-click #(rf/dispatch [:close-users-modal])} "Cancel"]]]]]
         [:button.modal-close.is-large
          {:aria-label "close" :on-click #(rf/dispatch [:close-users-modal])} "close"]]))))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

;; TODOO: delete this once all good
(defn view-list []
  (let [list @(rf/subscribe [:active-list])
        user @(rf/subscribe [:user])
        is-loading @(rf/subscribe [:loading-list])
        loading-translation @(rf/subscribe [:loading-translation])]
    (when list
      [:div.has-text-centered
       (when is-loading
         [:div.modal.is-active
          [:div.modal-background]
          [:div.modal-content>section.section
           [:div.has-text-centered.is-size-3.m-6>p.has-text-info "Loading List..."]
           [:progress.progress.is-info]]])
       [:p.is-size-2 (:name list)]
       [:p.is-size-4 (str "Created by: " (:creator list))]
       [:div.columns.m-2
        [:div.column]
        [:div.column
         [:p.is-size-6 "Shared with:"]
         (if (empty? (:users list))
           [:p.is-size-5.is-italic "No one"]
           [:p.is-size-5 (:users list)])]
        (when (= (:username user) (:creator list))
          [:div.column>a.button.is-info {:on-click #(rf/dispatch [:open-users-modal])} "Edit Sharing"])
        [:div.column]]
       [:section.section.p-1.m-1
        [:div.columns.is-centered
         [:div.column.is-narrow
          [:table.table.is-bordered.is-narrow.is-striped.is-hoverable
           [:thead [:tr
                    [:th "ID"]
                    [:th "Translated By"]
                    [:th (:source_language list)]
                    [:th (:target_language list)]
                    [:th "Audio?"]]]
           [:tbody
            (for [s (:translations list)]
              ^{:key (:id s)}
              [:tr
               [:td (:list_index s)]
               [:td (:translator s)]
               [:td [wrapped-string (:source_text s)]]
               [:td
                [wrapped-string (:target_text s)]
                [:br]
                [wrapped-string (:target_text_roman s)]]
               [:td (if (:has_audio s)
                      [:span.icon.has-text-success>i.fa.fa-check]
                      [:span.icon.has-text-danger>i.fa.fa-ban])]
               [:td [:a.button.is-info
                     (let [init {:on-click #(rf/dispatch [:fetch-translation (:id s)])}]
                       (if loading-translation
                         (if (= (:id s) (last loading-translation))
                           (assoc init :class :is-loading)
                           (assoc init :disabled :disabled))
                         init))
                     "edit"]]
               [:td [:a.button.is-danger
                     (let [init {:on-click #(rf/dispatch [:set-delete-translation-id s])}]
                       (if loading-translation
                         (if (= (:id s) (last loading-translation)) ;; TODO: need this ?
                           (assoc init :class :is-loading)
                           (assoc init :disabled :disabled))
                         init))
                     "delete"]]])]]]]]])))

(defn page []
  (when-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]
     [translate-modal]
     [assign-modal]
     [create-deck-modal]
     [delete-modal
      :delete-list-id
      :clear-delete-list-id
      :delete-list
      :name]
     [delete-modal
      :delete-translation-id
      :clear-delete-translation-id
      :delete-translation
      :source_text]
     [download-modal]
     [footer]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'dashboard-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/login" {:name        :login
               :view        #'login-page
               :controllers [{:start (fn [_] (rf/dispatch [:redirect-if-logged-in]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/lists/edit/:id" {:name :view-list
                        :view #'edit-list
                        :controllers [{:parameters {:path [:id]}
                                       :start (fn [{{:keys [id]} :path}]
                                                (rf/dispatch [:load-list-page id]))}]}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (rf/dispatch-sync [:init-local-storage])
  (mount-components))
