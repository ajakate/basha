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
   [basha.modals.translate :refer [translate-modal]]
   [basha.modals.share :refer [share-modal]]))

;; TODOO: DELETE MEEEE
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

(defn page []
  (when-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]
     [translate-modal]
     [share-modal]
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
