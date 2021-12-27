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
                    :header "Log In"
                    :dispatch :login}
        signup-vals {:swap-message "Log in to an existing account"
                     :button "Sign Up"
                     :swap-func [:set-signup false]
                     :header "Create an Account"
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
      [:header.card-header
       [:p.card-header-title (modal-vals :header)]]
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
           [:div.control>a.button.is-info
            {:on-click #(swap! show_pass not)}
            [:i.fa {:class (if @show_pass :fa-eye-slash :fa-eye)}]]]
          (if error [:p.has-text-danger.is-italic.mb-3 (str "Error: " error)])
          [:div.control>button.button.is-link
           {:on-click #(rf/dispatch [(modal-vals :dispatch) {:username @draft_user :password @draft_pass}])
            :disabled (or (string/blank? @draft_user)
                          (string/blank? @draft_pass))} (modal-vals :button)]
          [:br]
          [:a {:on-click #(rf/dispatch (modal-vals :swap-func))} (modal-vals :swap-message)]])]]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-login-modal])} "close"]]))

(defn translate-modal []
  (let [is-active @(rf/subscribe [:translate-modal-visible])
        translation @(rf/subscribe [:active-translation])
        list @(rf/subscribe [:active-list])
        loading-translation @(rf/subscribe [:loading-translation])]
    (if loading-translation
      [:div "loading"]
      [:div.modal
       {:class (if is-active "is-active" nil)}
       [:div.modal-background]
       [:div.model-content>div.card
        [:header.card-header
         [:p.card-header-title "Translate Sentence"]]
        [:div.card-content
         (r/with-let [draft_source (r/atom (:source_text translation))
                    ; TODO: add logic for source romainization
                      draft_target (r/atom nil)
                      draft_target_rom (r/atom nil)]
           [:div
            [:label.label "Source sentence"]
            [:div.field
             [:div.control>input.input
              {:type "text"
               :placeholder "sk8hkr69"
               :on-change #(reset! draft_source (.. % -target -value))
               :value @draft_source}]]
            [:label.label "Translation (native script)"]
            [:div.field
             [:div.control [:input.input
                            {:type "text"
                             :placeholder "type your native translation here"
                             :on-change #(reset! draft_target (.. % -target -value))
                             :value @draft_target}]]]
            [:label.label "Translation (roman script)"]
            [:div.field
             [:div.control [:input.input
                            {:type "text"
                             :placeholder "type your roman translation here"
                             :on-change #(reset! draft_target_rom (.. % -target -value))
                             :value @draft_target_rom}]]]
            [:div.control>button.button.is-link
             {:on-click #(rf/dispatch [:edit-translation {:target_text_roman @draft_target_rom
                                                          :target_text @draft_target
                                                          :source_text @draft_source
                                                          :id (:id translation)}])
              ; TODO: fix disabled here
              :disabled (or (string/blank? "sdf")
                            (string/blank? "sdf"))} "Submit"]])]]
       [:button.modal-close.is-large
        {:aria-label "close" :on-click #(rf/dispatch [:close-translate-modal])} "close"]])))

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

;; TODO: disallow nils in select
(defn create-list-page []
  (r/with-let [draft_name (r/atom nil)
               draft_source (r/atom nil)
               draft_target (r/atom nil)
               draft_file (r/atom nil)]
    [:div.px-6
     [:p.is-size-2 "Create a new sentence list"]
     [:div.field
      [:label.label "Name"]
      [:div.control>input.input
       {:type "text"
        :placeholder "My new sentce list"
        :on-change #(reset! draft_name (.. % -target -value))
        :value @draft_name}]]
     [:div.field
      [:label.label "Source Language"]
      [:div.control>div.select
       [:select {:on-change #(reset! draft_source (.. % -target -value))}
        [:option "English"]
        [:option "Marathi"]]]]
     [:div.field
      [:label.label "Target Language"]
      [:div.control>div.select
       [:select {:on-change #(reset! draft_target (.. % -target -value))}
        [:option "English"]
        [:option "Marathi"]]]]
     [:label.label "Upload a .txt file"]
     [:div.file.has-name
      [:label.file-label
       [:input.file-input {:type "file" :name "list" :on-change #(reset! draft_file (-> % .-target .-files (aget 0)))}]
       [:span.file-cta
        [:span.file-icon>i.fa.fa-upload]
        [:span.file-label "Choose a file"]]
       [:span.file-name (if @draft_file (.. @draft_file -name) "example.txt")]]]
     [:br]
     [:div.control>button.button.is-link
      {:on-click #(rf/dispatch [:create-list {:name @draft_name :source_language @draft_source :target_language @draft_target :file  @draft_file}])
       :disabled (or (string/blank? "fd")
                     (string/blank? "fd"))} "Create List"]]))

(defn view-list []
  (let [list @(rf/subscribe [:active-list])]
    (if list
      [:div
       [:h1 (:name list)]
       [:h2 (str "creator: " (:creator list))]
       [:table.table.is-bordered.is-narrow.is-striped.is-hoverable
        [:thead [:tr
                 [:th (:source_language list)]
                 [:th (:target_language list)]]]
        [:tbody
         (for [s (:translations list)]
           ^{:key (:id s)}
           [:tr
            [:td (:source_text s)]
            [:td (:target_text s)]
            [:td [:a.button.is-info {:on-click #(rf/dispatch [:open-translate-modal (:id s)])} "edit"]]])]]])))

(defn my-lists []
  (let [lists @(rf/subscribe [:list-summary])]
    (if (seq lists)
      
      [:table.table.is-bordered.is-narrow.is-striped.is-hoverable
       [:thead [:tr 
                [:th "name"]
                [:th "owner"] 
                [:th "source language"]
                [:th "target language"]
                [:th "sentence count"]
                ]]
       [:tbody
        (for [list lists]
          ^{:key (:id list)}
          [:tr
           [:td (:name list)]
           [:td (:creator list)]
           [:td (:source_language list)]
           [:td (:target_language list)]
           [:td (:list_count list)]
           [:td [:a.button.is-info {:href (str "/#/lists/edit/" (:id list))} "edit"]]])]]
      [:h1 "You don't have any lists!"])))

(defn logged-in-home []
  [:div.has-text-centered
   [:p.is-size-1 "My Dashboard"]
   [:a.button.is-primary {:href "/#/lists/new"} "Create a New Sentence List"]
   [my-lists]])

(defn logged-out-home []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn home-page []
  (let [user @(rf/subscribe [:user])]
    (if (seq user)
      [logged-in-home]
      [logged-out-home])))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]
     [login-modal]
     [translate-modal]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/lists/new" {:name :create-list
               :view #'create-list-page}]
    ["/lists/edit/:id" {:name :view-lsit
                    :view #'view-list
                    :controllers [{:parameters {:path [:id]}
                                   :start (fn [{{:keys [id]} :path}]
                                            ;; (rf/dispatch [:set-track-loading true])
                                            (rf/dispatch [:fetch-list id]))}]}]]))

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
