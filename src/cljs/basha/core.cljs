(ns basha.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [re-frame.core :as rf]
   [basha.ajax :as ajax]
   [basha.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [basha.modals.delete :refer [delete-modal]]
   [basha.modals.create-deck :refer [create-deck-modal]]
   [basha.layout.navbar :refer [navbar]]
   [basha.layout.footer :refer [footer]]
   [basha.pages.login :refer [login-page]]
   [basha.pages.dashboard :refer [dashboard-page]]
   [basha.pages.edit-list :refer [edit-list]]
   [basha.pages.invite :refer [invite-page]]
   [basha.modals.download :refer [download-modal]]
   [basha.modals.translate :refer [translate-modal]]
   [basha.modals.share :refer [share-modal]]))

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
                                                (rf/dispatch [:load-list-page id]))}]}]
    ["/share/:code" {:name :invite
                     :view #'invite-page
                     :controllers [{:parameters {:path [:code]}
                                    :start (fn [{{:keys [code]} :path}]
                                             (rf/dispatch [:fetch-invite code]))}]}]]))

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
