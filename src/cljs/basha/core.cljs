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
   [basha.languages :as bl]
   [clojure.string :as string]))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn format-string [st]
  (let [words (map #(str % \space) (string/split st #" "))]
    (partition 10 10 nil words)))

(defn wrapped-string [st]
  [:div
   (for [l (format-string st)]
     ^{:key (str l)}
     [:p l])])

(defn delete-list-modal []
  (let [active-list @(rf/subscribe [:delete-list-id])
        id (:id active-list)
        name (:name active-list)]
    (when active-list
      [:div.modal
       {:class (if active-list "is-active" nil)}
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
           {:on-click #(rf/dispatch [:delete-list id])} "Delete"]
          [:div.column.control>button.button
           {:on-click #(rf/dispatch [:clear-delete-list-id])} "Cancel"]]]]])))

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

(defn login-modal []
  (let [is-active @(rf/subscribe [:login-modal-visible])
        error @(rf/subscribe [:login-errors])]
    (when is-active
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
        {:aria-label "close" :on-click #(rf/dispatch [:close-login-modal])} "close"]])))

(defn recording-state-component [state temp existing-audio]
  (let [state (or state :init)
        components {:armed [:nav.level
                            [:div.level-left]
                            [:div.level-right
                             [:button.button.p-2.m-1.is-primary.level-item
                              {:on-click #(rf/dispatch [:start-recording])} [:i.fa.fa-microphone.m-1] [:span "Record Now"]]]]
                    :recording [:div
                                [:nav.level.mb-0
                                 [:div.level-left
                                  [:p.level-item.is-italic.has-text-weight-bold.ml-4 [:span.blink "RECORDING..."]]]]
                                [:nav.level
                                 [:div.level-left.mr-2
                                  [:progress.progress.is-danger.is-large.level-item]]
                                 [:div.level-right
                                  [:button.button.p-2.m-1.is-danger.level-item
                                   {:on-click #(rf/dispatch [:stop-recording])} [:i.fa.fa-stop.m-1] [:span "Finish Recording"]]
                                  [:button.button.p-2.m-1.level-item
                                   {:on-click #(rf/dispatch [:cancel-recording])} "Cancel"]]]]
                    :stopped [:div
                              [:nav.level
                               [:div.level-left
                                [:audio.level-item {:controls "controls" :autoplay "autoplay" :src (:url temp)}]]
                               [:div.level-right
                                [:button.button.p-2.m-1.level-item.is-primary
                                 {:on-click #(rf/dispatch [:arm-recording])} [:i.fa.fa-repeat.m-1] [:span "Re-record"]]
                                [:button.button.p-2.m-1.level-item
                                 {:on-click #(rf/dispatch [:cancel-recording])} "Delete"]]]
                              (when existing-audio
                                [:div.notification.is-success.is-light
                                 [:span.has-text-weight-bold "Tip: "]
                                 [:span "Click either "]
                                 [:span.has-text-weight-bold "Save"]
                                 [:span " button at the bottom of this pop-up to save your new audio."]
                                 [:br]
                                 [:span "Click the "]
                                 [:span.has-text-weight-bold "Delete"]
                                 [:span " button above to keep the original audio instead."]])]}]
    (state components)))

(defn translate-modal []
  (let [is-active @(rf/subscribe [:translate-modal-visible])
        translation @(rf/subscribe [:active-translation])
        list @(rf/subscribe [:active-list])
        target-lang (:target_language list)
        loading-translation @(rf/subscribe [:loading-translation])
        recording-state @(rf/subscribe [:recording-state])
        temp-recording @(rf/subscribe [:temp-recording])
        next-id (:next_id translation)
        media-error @(rf/subscribe [:media-error])
        hide-native @(rf/subscribe [:hide-native])]
    [:div.modal
     {:class (if is-active "is-active" nil)}
     [:div.modal-background]
     [:div.model-content>div.card
      [:header.card-header
       [:p.card-header-title "Translate Sentence"]]
      [:div.card-content
       (if loading-translation
         [:section.section
          [:div.has-text-centered.is-size-3.m-6>p.has-text-info "Loading Next Translation..."]
          [:progress.progress.is-info]]
         (r/with-let [draft_target (r/atom (:target_text translation))
                      draft_target_rom (r/atom (:target_text_roman translation))]
           [:div
            [:label.label "Source sentence"]
            [:div.box.my-5 [wrapped-string (:source_text translation)]]
            [:label.label "Translated Audio"]
            (when-let [audio (:audio translation)]
              [:div.box.p-3
               [:label.label "Existing Audio"]
               [:div.columns
                [:div.column
                 [:audio {:controls "controls" :autoplay "autoplay" :src (str "data:audio/ogg;base64," audio)}]]
                [:div.column>button.button.is-danger.is-pulled-right
                 {:on-click #(rf/dispatch [:delete-audio (:id translation)])} "Delete Audio"]]])
            [:div.box.p-3
             [:label.label "New Audio"]
             (if media-error
               [:div.is-italic.has-text-danger [wrapped-string media-error]]
               [recording-state-component recording-state temp-recording (:audio translation)])]
            (when-not (bl/has-latin-script target-lang)
              (if hide-native
                [:button.button.is-pulled-right.is-small.is-link.is-light.ml-4.mb-2
                 {:on-click #(rf/dispatch [:swap-hide-native])}
                 "Show native script"]
                [:div
                 [:label.label.is-pulled-left
                  [:span.is-italic "(Optional) "]
                  [:span "Translation - native script"]]
                 [:button.button.is-pulled-right.is-small.is-link.is-light.ml-4.mb-2
                  {:on-click #(rf/dispatch [:swap-hide-native])}
                  "Hide native script"]
                 [:div.field
                  [:div.control [:input.input
                                 {:type "text"
                                  :placeholder "मार्टिन फॉलर "
                                  :on-change #(reset! draft_target (.. % -target -value))
                                  :value @draft_target}]]]]))
            [:label.label
             [:span "Translation"]
             (when-not (bl/has-latin-script target-lang) [:span " - latin script"])]
            [:div.field
             [:div.control [:input.input
                            {:type "text"
                             :placeholder "Kasa kay mandali"
                             :on-change #(reset! draft_target_rom (.. % -target -value))
                             :value @draft_target_rom}]]]
            [:div.columns.mt-4
             [:div.column.has-text-centered.control>button.button.is-link
              {:on-click #(rf/dispatch [:edit-translation {:target_text_roman @draft_target_rom
                                                           :target_text @draft_target
                                                           :id (:id translation)
                                                           :list_id (:id list)
                                                           :audio (:data temp-recording)
                                                           :goto-next true
                                                           :next_id next-id}])
               :disabled (= recording-state :recording)
               :class (if loading-translation :is-loading nil)
               :style (if (:next_id translation) nil {:visibility :hidden})} "Save & Next"]
             [:div.column.has-text-centered.control>button.button.is-link
              {:on-click #(rf/dispatch [:edit-translation {:target_text_roman @draft_target_rom
                                                           :target_text @draft_target
                                                           :id (:id translation)
                                                           :list_id (:id list)
                                                           :audio (:data temp-recording)}])
               :disabled (= recording-state :recording)
               :class (if loading-translation :is-loading nil)} "Save & Close"]
             [:div.column.has-text-centered.control>button.button
              {:on-click #(rf/dispatch [:close-translate-modal])
               :class (if loading-translation :is-loading nil)} "Cancel"]]]))]]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-translate-modal])} "close"]]))

(defn navbar []
  ;; TODO: fix logic seq user nil
  (r/with-let [expanded? (r/atom false)]
    (let [user @(rf/subscribe [:user])
          user-exists (seq user)]
      [:nav.navbar.is-info>div.container
       [:div.navbar-brand
        [:a.navbar-item {:style (if user-exists {:font-weight :bold :pointer-events :none} {:font-weight :bold})
                         :on-click #(rf/dispatch [:open-login-modal])}
         (if user-exists (str "Hi, " (:username user) "!") "log in")]
        [:span.navbar-burger.burger
         {:data-target :nav-menu
          :on-click #(swap! expanded? not)
          :class (when @expanded? :is-active)}
         [:span] [:span] [:span]]]
       [:div#nav-menu.navbar-menu
        {:class (when @expanded? :is-active)}
        [:div.navbar-start
         [nav-link "#/" (if user-exists "My Dashboard" "Home") :home]
         [nav-link "#/about" "About" :about]
         (when user-exists [:a.navbar-item
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
    (let [error @(rf/subscribe [:create-list-error])
          loading @(rf/subscribe [:loading-create-list])]
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
          (for [l bl/language-list]
            [:option l])]]]
       [:div.field
        [:label.label "Target Language"]
        [:div.control>div.select
         [:select {:on-change #(reset! draft_target (.. % -target -value))}
          [:option "Select"]
          (for [l bl/language-list]
            [:option l])]]]
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
        {:class (when loading :is-loading)
         :on-click #(rf/dispatch [:create-list {:name @draft_name
                                                :source_language @draft_source
                                                :target_language @draft_target
                                                :file  @draft_file}])
         :disabled (or (string/blank? @draft_name)
                       (string/blank? @draft_source)
                       (string/blank? @draft_target)
                       (nil? @draft_file))} "Create List"]
       (when error
         [:div
          [:br]
          [:p.has-text-danger.is-italic.mb-3
           "Your sentence list failed to create. Make sure you are using a plain text file with UTF encoding."]])])))

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
                     "edit"]]])]]]]]])))

(defn my-lists []
  (let [lists @(rf/subscribe [:list-summary])
        user @(rf/subscribe [:user])]
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
                    [:th "shared with"]
                    [:th "source"]
                    [:th "target"]
                    [:th "total count"]
                    [:th "remaining count"]]]
           [:tbody
            (for [list lists]
              ^{:key (:id list)}
              [:tr
               [:td (:name list)]
               [:td (:creator list)]
               [:td (:users list)]
               [:td (:source_language list)]
               [:td (:target_language list)]
               [:td (:list_count list)]
               [:td
                (let [count (:open_count list)]
                  (if (= count 0)
                    [:div.has-text-success
                     [:span count]
                     [:span.icon>i.fa.fa-check]]
                    [:span count " left"]))]
               [:td>a.button.is-info
                {:href (str "/#/lists/edit/" (:id list))}
                "edit"]
               (when (= (:username user) (:creator list))
                 [:td>a.button.is-danger.is-light
                  {:on-click #(rf/dispatch [:set-delete-list-id list])}
                  "delete"])])]]]]]]
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
     [assign-modal]
     [delete-list-modal]]))

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
