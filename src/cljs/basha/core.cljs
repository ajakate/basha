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
   [basha.languages :as bl]
   [clojure.string :as string]
   [basha.modals.delete :refer [delete-modal]]
   [basha.modals.create-deck :refer [create-deck-modal]]
   [basha.layout.navbar :refer [navbar]]
   [basha.layout.footer :refer [footer]]
   [basha.pages.login :refer [login-page]]
   [basha.pages.dashboard :refer [dashboard-page]]
   [basha.pages.edit-list :refer [edit-list]]))

;; TODOO: DELETE THESE!!!!!
(defn format-string [st]
  (let [words (map #(str % \space) (string/split st #" "))]
    (partition 10 10 nil words)))

(defn wrapped-string [st]
  [:div
   (for [l (format-string st)]
     ^{:key (str l)}
     [:p l])])

(defn download-modal []
  (let [is-active @(rf/subscribe [:is-downloading])
        errors @(rf/subscribe [:download-error])]
    (when is-active
      [:div.modal
       {:class (if is-active "is-active" nil)}
       [:div.modal-background]
       [:div.model-content>div.card
        [:header.card-header>p.card-header-title "Downloading Anki Deck..."]
        [:div.card-content.has-text-centered
         (if errors
           [:div
            [:p.has-text-danger.is-italic.my-4 errors]
            [:button.button.p-2.m-1
             {:on-click #(rf/dispatch [:kill-download-modal])}
             "Cancel"]]
           [:div
            [:span "Please don't navigate away from this page..."]
            [:progress.progress.is-primary.is-large.my-4]])]]])))

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
                      draft_target_rom (r/atom (:target_text_roman translation))
                      draft_source (r/atom (:source_text translation))
                      editing-source (r/atom false)]
           [:div
            [:label.label "Source sentence"]
            [:button.button.is-pulled-right.is-small.is-primary.ml-4.mb-4
             {:on-click  #(swap! editing-source not)}
             (if @editing-source "Done editing" "Edit Source Sentence")]
            (if @editing-source
              [:div.field
               [:div.control [:input.input
                              {:type "text"
                               :placeholder "What's up?"
                               :on-change #(reset! draft_source (.. % -target -value))
                               :value @draft_source}]]]
              [:div.box.my-5 [wrapped-string @draft_source]])
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
            (let [base-params {:source_text @draft_source
                               :target_text_roman @draft_target_rom
                               :target_text @draft_target
                               :id (:id translation)
                               :list_id (:id list)
                               :audio (:data temp-recording)}]
              [:div.columns.mt-4
               [:div.column.has-text-centered.control>button.button.is-link
                {:on-click #(rf/dispatch [:edit-translation (assoc base-params :goto-next true :next_id next-id)])
                 :disabled (= recording-state :recording)
                 :class (if loading-translation :is-loading nil)
                 :style (if (:next_id translation) nil {:visibility :hidden})} "Save & Next"]
               [:div.column.has-text-centered.control>button.button.is-link
                {:on-click #(rf/dispatch [:edit-translation base-params])
                 :disabled (= recording-state :recording)
                 :class (if loading-translation :is-loading nil)} "Save & Close"]
               [:div.column.has-text-centered.control>button.button
                {:on-click #(rf/dispatch [:close-translate-modal])
                 :class (if loading-translation :is-loading nil)} "Cancel"]])]))]]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-translate-modal])} "close"]]))

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
