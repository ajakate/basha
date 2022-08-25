(ns basha.modals.create-deck
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [basha.languages :as bl]
   [clojure.string :as string]
   [basha.events]))

;; TODOO: add link to community decks
;; TODOO: fix modal close button
(defn create-deck-modal []
  (let [active @(rf/subscribe [:create-deck-modal-visible])]
    (when active
      (r/with-let [draft_name (r/atom nil)
                   draft_target (r/atom nil)
                   draft_file (r/atom nil)] 
        (let [error @(rf/subscribe [:create-list-error])
              loading @(rf/subscribe [:loading-create-list])]
          [:div.modal
           {:class (if active "is-active" nil)}
           [:div.modal-background]
           [:div.model-content>div.card.has-background-white.has-text-centered
            [:div.p-6
             [:img.my-3 {:src "img/deck_icon.png"}]
             [:h2.has-text-orange.is-size-3.mb-4 "Create a New Deck"]
             [:p.pb-4 "Upload your deck as a plain-text (.txt) file"]
             [:div.has-text-left
              [:p.py-2.bold "Name"]
              [:input.input
               {:type "text"
                :placeholder "My new sentence list"
                :on-change #(reset! draft_name (.. % -target -value))
                :value @draft_name}]
              [:p.py-2.mt-3.bold "Language"]
              [:div.control>div.select
               [:select {:on-change #(reset! draft_target (.. % -target -value))}
                [:option "Select"]
                (for [l bl/language-list]
                  [:option l])]]
              [:p.py-2.mt-3.bold "Deck File"]
              [:div.file.has-name
               [:label.file-label
                [:input.file-input {:type "file" :name "list" :on-change #(reset! draft_file (-> % .-target .-files (aget 0)))}]
                [:span.file-cta
                 [:span.file-icon>i.fa.fa-upload]
                 [:span.file-label "Choose a file"]]
                [:span.file-name (if @draft_file (.. @draft_file -name) [:i "No file selected"])]]]]
             [:button.button.is-orange.bold.mt-5
              {:class (when loading :is-loading)
               :on-click #(rf/dispatch [:create-list {:name @draft_name
                                                      :source_language "English"
                                                      :target_language @draft_target
                                                      :file  @draft_file}])
               :disabled (or (string/blank? @draft_name)
                             (string/blank? @draft_target)
                             (nil? @draft_file))} "Create List"]
             (when error
               [:div
                [:br]
                [:div.has-text-danger.is-italic.mb-3
                 [:p.mb-2 "Your sentence list failed to create..."]
                 [:p "Please make sure you are uploading"]
                 [:p.bold "a plain text file between 1 and 500 lines"]]])
             [:button.modal-close.is-large
              {:aria-label "close" :on-click #(rf/dispatch [:create-deck-modal-visible false])} "close"]]]])))))