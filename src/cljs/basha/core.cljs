(ns basha.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [basha.ajax :as ajax]
   [basha.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn modal-vals [arg]
  (let [signup @(rf/subscribe [:is-signup])
        login-vals {:swap-message "Create an account"
                    :button "Log In"
                    :swap-func [:set-signup true]
                    :dispatch :login}
        signup-vals {:swap-message "Log in to an existing account"
                     :button "Sign Up"
                     :swap-func [:set-signup false]
                     :dispatch :signup}]
    (if signup
      (arg signup-vals)
      (arg login-vals))))

;; TODO: add confirm logic, add error presenting
(defn login-modal []
  (let [is-active @(rf/subscribe [:login-modal-visible])
        is-signup @(rf/subscribe [:is-signup])
        error @(rf/subscribe [:login-errors])]
    [:div.modal
     {:class (if is-active "is-active" nil)}
     [:div.modal-background]
     [:div.model-content>div.card
      [:div.card-content
       (r/with-let [draft_user (r/atom nil)
                    draft_pass (r/atom nil)
                    show_pass (r/atom false)]
         [:div
          [:div.field
           [:label.label "Username"]
           [:div.control>input.input
            {:type "text"
             :placeholder "sk8hkr69"
             :on-change #(reset! draft_user (.. % -target -value))
             :value @draft_user}]]
          [:label.label "Password"]
          [:div.field.has-addons
           [:div.control [:input.input
                          {:type (if @show_pass "text" "password")
                           :placeholder "test123"
                           :on-change #(reset! draft_pass (.. % -target -value))
                           :value @draft_pass}]]
           [:div.control>a.button.is-info {:on-click #(reset! show_pass (not @show_pass))} (if @show_pass "hide password" "show password")]]
          (if error [:p.has-text-danger.is-italic.mb-3 (str "Error: " error)])
          [:div.control>button.button.is-link
           {:on-click #(rf/dispatch [(modal-vals :dispatch) {:username @draft_user :password @draft_pass}])
            :disabled (or (string/blank? @draft_user)
                          (string/blank? @draft_pass))} (modal-vals :button)]
          [:br]
          [:a {:on-click #(rf/dispatch (modal-vals :swap-func))} (modal-vals :swap-message)]])]]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-login-modal])} "close"]]))

(defn navbar []
  ;; TODO: fix logic seq user nil
  (r/with-let [expanded? (r/atom false)]
    (let [user @(rf/subscribe [:user])]
      [:nav.navbar.is-info>div.container
       [:div.navbar-brand
        [:a.navbar-item {:style (if (seq user) {:font-weight :bold :pointer-events :none} {:font-weight :bold})
                         :on-click #(rf/dispatch [:swap-login-modal true])}
         (if (empty? user) "log in" (str "Hi, " (:username user) "!"))]
        [:span.navbar-burger.burger
         {:data-target :nav-menu
          :on-click #(swap! expanded? not)
          :class (when @expanded? :is-active)}
         [:span] [:span] [:span]]]
       [:div#nav-menu.navbar-menu
        {:class (when @expanded? :is-active)}
        [:div.navbar-start
         [nav-link "#/" "Home" :home]
         [nav-link "#/about" "About" :about]
         (if (seq user) [:a.navbar-item
                   {:on-click #(rf/dispatch [:logout])}
                   "Logout"])]]])))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]
     [login-modal]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view #'about-page}]]))

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
