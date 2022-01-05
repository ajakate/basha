(ns basha.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [markdown.core :refer [md->html]]
   [basha.ajax :as ajax]
   [basha.events]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

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
          (when error [:p.has-text-danger.is-italic.mb-3 (str "Error: " error)])
          [:div.control>button.button.is-link
           {:on-click #(rf/dispatch [(modal-vals :dispatch) {:username @draft_user :password @draft_pass}])
            :disabled (or (string/blank? @draft_user)
                          (string/blank? @draft_pass))} (modal-vals :button)]
          [:br]
          [:a {:on-click #(rf/dispatch (modal-vals :swap-func))} (modal-vals :swap-message)]])]]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-login-modal])} "close"]]))

(defn recording-state-component [state temp]
  (let [state (or state :init)
        components {:init [:nav.level
                           [:div.level-left]
                           [:div.level-right
                            [:div.column>button.button.p-2.m-1.is-primary.level-item
                             {:on-click #(rf/dispatch [:arm-recording])} "Record New Audio"]]]
                    :armed [:nav.level
                            [:div.level-left
                             [:div.level-item "Get ready to start speaking ->"]]
                            [:div.level-right
                             [:button.button.p-2.m-1.is-primary.level-item
                              {:on-click #(rf/dispatch [:start-recording])} "Start Recording"]
                             [:button.button.p-2.m-1.level-item
                              {:on-click #(rf/dispatch [:cancel-recording])} "Cancel"]]]
                    :recording [:nav.level
                                [:div.level-left 
                                 [:progress.progress.is-danger.level-item]]
                                [:div.level-right
                                 [:button.button.p-2.m-1.is-danger.level-item
                                  {:on-click #(rf/dispatch [:stop-recording])} "Finish Recording"]
                                 [:button.button.p-2.m-1.level-item
                                  {:on-click #(rf/dispatch [:cancel-recording])} "Cancel"]]]
                    :stopped [:div
                              [:nav.level
                               [:div.level-left
                                [:audio.level-item {:controls "controls" :src (:url temp)}]]
                               [:div.level-right
                                [:button.button.p-2.m-1.level-item
                                 {:on-click #(rf/dispatch [:cancel-recording])} "Cancel"]]]
                              [:p "You can submit this new audio by hitting the 'Save' button below."]
                              [:p "If you'd rather keep the existing audio (if it's there) hit 'Cancel' above."]]
                    }]
    (state components)))

(defn translate-modal []
  (let [is-active @(rf/subscribe [:translate-modal-visible])
        translation @(rf/subscribe [:active-translation])
        list @(rf/subscribe [:active-list])
        loading-translation @(rf/subscribe [:loading-translation])
        recording-state @(rf/subscribe [:recording-state])
        temp-recording @(rf/subscribe [:temp-recording])
        next-id (:next_id translation)]
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
                      draft_target (r/atom (:target_text translation))
                      draft_target_rom (r/atom (:target_text_roman translation))]
           [:div
            [:label.label "Source sentence"]
            [:div.field
             [:div.control>input.input
              {:type "text"
               :placeholder "sk8hkr69"
               :on-change #(reset! draft_source (.. % -target -value))
               :value @draft_source}]]
            [:label.label "Translated Audio"]
            (when-let [audio (:audio translation)]
              [:div.box.p-3
               [:label.label "Existing Audio"]
               [:div.columns
                [:div.column
                 [:audio {:controls "controls" :src (str "data:audio/ogg;base64," audio)}]]
                [:div.column>button.button.is-danger
                 {:on-click #(rf/dispatch [:delete-audio (:id translation)])} "Delete Audio"]]])
            [:div.box.p-3
             [:label.label "Record New Audio"]
             [recording-state-component recording-state temp-recording]]
            [:label.label "Translation (native script)"]
            [:div.field
             [:div.control [:input.input
                            {:type "text"
                             :placeholder "मार्टिन फॉलर "
                             :on-change #(reset! draft_target (.. % -target -value))
                             :value @draft_target}]]]
            [:label.label "Translation (roman script)"]
            [:div.field
             [:div.control [:input.input
                            {:type "text"
                             :placeholder "Kasa kay mandali"
                             :on-change #(reset! draft_target_rom (.. % -target -value))
                             :value @draft_target_rom}]]]
            [:div.columns
             [:div.column.has-text-centered.control>button.button.is-link
              {:on-click #(rf/dispatch [:edit-translation {:target_text_roman @draft_target_rom
                                                           :target_text @draft_target
                                                           :source_text @draft_source
                                                           :id (:id translation)
                                                           :list_id (:id list)
                                                           :audio (:data temp-recording)
                                                           :goto-next true
                                                           :next_id next-id}])
               :disabled (= recording-state :recording)
               :style (if (:next_id translation) nil {:visibility :hidden})} "Save & Next"]
             [:div.column.has-text-centered.control>button.button.is-link
              {:on-click #(rf/dispatch [:edit-translation {:target_text_roman @draft_target_rom
                                                           :target_text @draft_target
                                                           :source_text @draft_source
                                                           :id (:id translation)
                                                           :list_id (:id list)
                                                           :audio (:data temp-recording)}])
               :disabled (= recording-state :recording)} "Save & Exit"]
             [:div.column.has-text-centered.control>button.button {:on-click #(rf/dispatch [:close-translate-modal])} "Cancel"]]])]]
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
         (when (seq user) [:a.navbar-item
                   {:on-click #(rf/dispatch [:logout])}
                   "Logout"])]]])))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn create-list-page []
  (r/with-let [draft_name (r/atom nil)
               draft_source (r/atom nil)
               draft_target (r/atom nil)
               draft_file (r/atom nil)]
    (let [error @(rf/subscribe [:create-list-error])]
      [:div.px-6
       [:p.is-size-2 "Create a new sentence list"]
       [:div.field
        [:label.label "Name"]
        [:div.control>input.input
         {:type "text"
          :placeholder "My new sentence list"
          :on-change #(reset! draft_name (.. % -target -value))
          :value @draft_name}]]
       [:div.field
        [:label.label "Source Language"]
        [:div.control>div.select
         [:select {:on-change #(reset! draft_source (.. % -target -value))}
          [:option "Select"]
          [:option "English"]
          [:option "Marathi"]]]]
       [:div.field
        [:label.label "Target Language"]
        [:div.control>div.select
         [:select {:on-change #(reset! draft_target (.. % -target -value))}
          [:option "Select"]
          [:option "English"]
          [:option "Marathi"]]]]
       [:label.label "Upload a .txt file"]
       [:div.file.has-name
        [:label.file-label
         [:input.file-input {:type "file" :name "list" :on-change #(reset! draft_file (-> % .-target .-files (aget 0)))}]
         [:span.file-cta
          [:span.file-icon>i.fa.fa-upload]
          [:span.file-label "Choose a file"]]
         [:span.file-name (if @draft_file (.. @draft_file -name) [:i "No file selected"])]]]
       [:br]
       [:div.control>button.button.is-link
        {:on-click #(rf/dispatch [:create-list {:name @draft_name
                                                :source_language @draft_source
                                                :target_language @draft_target
                                                :file  @draft_file}])
         :disabled (or (string/blank? @draft_name)
                       (string/blank? @draft_source)
                       (string/blank? @draft_target)
                       (nil? @draft_file))} "Create List"]
       [:p.has-text-danger.is-italic.mb-3 (str error)]])))

(defn view-list []
  (let [list @(rf/subscribe [:active-list])
        user @(rf/subscribe [:user])]
    (when list
      [:div.has-text-centered
       [:p.is-size-2 (:name list)]
       [:p.is-size-4 (str "Created by: " (:creator list))]
       [:div.columns.m-2
        [:div.column]
        [:div.column>p.is-size-4 (str "Shared with: "
                                      (if (empty? (:users list))
                                        "No one"
                                        (:users list)))]
        (when (= (:username user) (:creator list))
          [:div.column>a.button.is-info {:on-click #(rf/dispatch [:open-users-modal])} "Edit Sharing"])
        [:div.column]]
       [:section.section.p-1.m-1
        [:div.columns.is-centered
         [:div.column.is-narrow
          [:table.table.is-bordered.is-narrow.is-striped.is-hoverable
           [:thead [:tr
                    [:th "ID"]
                    [:th (:source_language list)]
                    [:th (:target_language list)]
                    [:th "Translated By"]
                    [:th "Audio?"]]]
           [:tbody
            (for [s (:translations list)]
              ^{:key (:id s)}
              [:tr
               [:td (:list_index s)]
               [:td (:source_text s)]
               [:td [:div (:target_text s) [:br] (:target_text_roman s)]]
               [:td (:translator s)]
               [:td (if (:has_audio s)
                      [:span.icon.has-text-success>i.fa.fa-check]
                      [:span.icon.has-text-danger>i.fa.fa-ban])]
               [:td [:a.button.is-info {:on-click #(rf/dispatch [:open-translate-modal (:id s)])} "edit"]]])]]]]]])))

(defn my-lists []
  (let [lists @(rf/subscribe [:list-summary])]
    (if (seq lists)
      [:div.p-2.m-2
       [:h1.title.is-4 "My Sentence Lists"]
       [:section.section.p-1.m-1
        [:div.columns.is-centered
         [:div.column.is-narrow
          [:table.table.is-bordered.is-narrow.is-striped.is-hoverable
           [:thead [:tr
                    [:th "name"]
                    [:th "owner"]
                    [:th "source language"]
                    [:th "target language"]
                    [:th "total count"]
                    [:th "open translations"]]]
           [:tbody
            (for [list lists]
              ^{:key (:id list)}
              [:tr
               [:td (:name list)]
               [:td (:creator list)]
               [:td (:source_language list)]
               [:td (:target_language list)]
               [:td (:list_count list)]
               [:td (:open_count list)]
               [:td [:a.button.is-info {:href (str "/#/lists/edit/" (:id list))} "edit"]]])]]]]]]
      [:h1 "You don't have any lists!"])))

(defn logged-in-home []
  [:div.has-text-centered
   [:p.is-size-1 "My Dashboard"]
   [:a.button.is-primary.m-3 {:href "/#/lists/new"} "Create a New Sentence List"]
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
  (when-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]
     [login-modal]
     [translate-modal]
     [assign-modal]]))

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
                   :view #'create-list-page
                   :controllers [{:start (fn []
                                           (rf/dispatch [:clear-create-list-error]))}]}]
    ["/lists/edit/:id" {:name :view-list
                    :view #'view-list
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
