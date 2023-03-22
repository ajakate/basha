(ns basha.modals.translate
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [basha.components.shared :refer [wrapped-string]]))

(defn display-audio-state [new-audio existing-audio recording-state]
  (if (= recording-state :recording)
    :display-recording
    (if new-audio
      :display-new-audio
      (if existing-audio
        :display-existing-audio
        :display-new))))

(defn translate-modal []
  (let [is-active @(rf/subscribe [:translate-modal-visible])
        translation @(rf/subscribe [:active-translation])
        list @(rf/subscribe [:active-list])
        loading-translation @(rf/subscribe [:loading-translation])
        recording-state @(rf/subscribe [:recording-state])
        temp-recording @(rf/subscribe [:temp-recording])
        next-id (:next_id translation)
        media-error @(rf/subscribe [:media-error])]
    [:div.modal
     {:class (if is-active "is-active" nil)}
     [:div.modal-background]
     [:div.modal-content>div.card.has-background-white.has-text-centered.p-5
      [:img.my-3.image.is-64x64.mx-auto {:src "img/translate_icon.png"}]
      [:p.has-text-blue.is-size-3.mb-4 "Translate Phrase"]
      (if loading-translation
        [:section.section
         [:div.has-text-centered.is-size-4.m-5>p.has-text-info "Loading Next Translation..."]
         [:div.flex.centered-text>div.is-size-1.loader-mixin]]
        (r/with-let [draft_target (r/atom (:target_text translation))
                     draft_target_rom (r/atom (:target_text_roman translation))
                     draft_source (r/atom (:source_text translation))
                     editing-source (r/atom false)]
          [:div.columns.is-centered.maxx>div.column.is-full>div.columns.is-multiline.has-text-left
           [:div.column.is-half.bold "Review Phrase"]
           [:div.column.is-half
            (if @editing-source
              [:input.input.mb-2
               {:type "text"
                :placeholder "What's up?"
                :on-change #(reset! draft_source (.. % -target -value))
                :value @draft_source}]
              [:p.is-inline-block [wrapped-string @draft_source]])
            [:button.button.is-small.is-outlined.is-blue.ml-2.p-2
             {:on-click  #(swap! editing-source not)}
             (if @editing-source
               [:div [:i.fa.fa-check] [:span.ml-1 "Done"]]
               [:div [:i.fa.fa-pencil] [:span.ml-1 "Edit"]])]]
           [:div.column.is-half.bold "Translate Text"]
           [:div.column.is-half
            [:input.input
             {:type "text"
              :placeholder "Kasa kay mandali"
              :on-change #(reset! draft_target_rom (.. % -target -value))
              :value @draft_target_rom}]]
           (when-not (:has_latin_script list)
             [:<>
              [:div.column.is-half.bold
               [:span.mr-3 "Notes / Context"]
               [:div.tooltip
                [:i.fa.fa-question-circle.tooltip]
                [:div.tooltiptext
                 [:p.mb-3 "Is there more than one way to translate this sentence?"]
                 [:p.mb-3 "Ex:"]
                 [:p.mb-3 "Speaking formally vs. informally"]
                 [:p.mb-3 "Male vs. female speaker"]
                 [:p.mb-3 "Speaking to a male vs. female person"]]]]
              [:div.column.is-half
               [:input.input
                {:type "text"
                 :placeholder "male speaker, formal, etc."
                 :on-change #(reset! draft_target (.. % -target -value))
                 :value @draft_target}]]])
           [:div.column.is-half.bold "Record Audio Translation"]
           (if media-error
             [:div.column.is-half.is-italic.has-text-danger media-error]
             [:div.column.is-half
              (case (display-audio-state temp-recording (:audio translation) recording-state)
                :display-new
                [:button.button.p-2.m-1.is-outlined.is-blue.is-small
                 {:on-click #(rf/dispatch [:start-recording])} [:i.fa.fa-microphone.m-1] [:span "Record Now"]]
                :display-existing-audio
                [:div
                 [:audio {:controls "controls" :autoplay "autoplay" :src (str "data:audio/ogg;base64," (:audio translation))}]
                 [:div.is-pulled-right
                  [:button.button.is-outlined.is-blue.is-small.mr-2
                   {:on-click #(rf/dispatch [:start-recording])}
                   "redo audio"]
                  [:button.button.is-outlined.is-blue.is-small
                   {:on-click #(rf/dispatch [:delete-audio (:id translation)])}
                   "delete audio"]]]
                :display-new-audio
                [:div
                 [:audio {:controls "controls" :autoplay "autoplay" :src (:url temp-recording)}]
                 [:div.is-pulled-right
                  [:button.button.is-outlined.is-blue.is-small.mr-2
                   {:on-click #(rf/dispatch [:start-recording])}
                   "redo audio"]
                  [:button.button.is-outlined.is-blue.is-small
                   {:on-click #(rf/dispatch [:cancel-recording])}
                   "clear new recording"]]]
                :display-recording
                [:div
                 [:div.columns.is-vcentered [:span.column.is-narrow.blink "RECORDING..."] [:div.column.is-narrow>progress.progress.is-blue.is-small]]
                 [:div.is-pulled-right
                  [:button.button.is-outlined.is-blue.is-small.mr-2
                   {:on-click #(rf/dispatch [:stop-recording])}
                   "finish"]
                  [:button.button.is-outlined.is-blue.is-small
                   {:on-click #(rf/dispatch [:cancel-recording])}
                   "cancel"]]])])
           (let [base-params {:source_text @draft_source
                              :target_text_roman @draft_target_rom
                              :target_text @draft_target
                              :id (:id translation)
                              :list_id (:id list)
                              :audio (:data temp-recording)}]
             [:<>
              [:div.column.is-one-third.has-text-centered>button.button.bold
               {:on-click #(rf/dispatch [:close-translate-modal])
                :class (if loading-translation :is-loading nil)}
               "Cancel"]
              [:div.column.is-one-third.has-text-centered>button.button.is-orange.bold
               {:on-click #(rf/dispatch [:edit-translation base-params])
                :class (if loading-translation :is-loading nil)
                :disabled (= recording-state :recording)}
               "Save and Close"]
              [:div.column.is-one-third.has-text-centered>button.button.is-green.bold.has-text-white
               {:on-click #(rf/dispatch [:edit-translation (assoc base-params :goto-next true :next_id next-id)])
                :class (if loading-translation :is-loading nil)
                :disabled (= recording-state :recording)
                :style (if (:next_id translation) nil {:visibility :hidden})}
               "Save and Next"]])]))]
     [:button.modal-close.is-large
      {:aria-label "close" :on-click #(rf/dispatch [:close-translate-modal])} "close"]]))
