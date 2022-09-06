(ns basha.pages.backup
  (:require
   [re-frame.core :as rf]
   [reagent.core :as r]
   [basha.components.shared :refer [is-admin loading-screen]]
   [reagent.core :as r]))

(defn backup-page []
  (let [loading-backup @(rf/subscribe [:loading-backup])
        info @(rf/subscribe [:info])]
    [:div.px-6>div.basha-panel.mx-auto.background-semi-faded.p-5
     [loading-screen :loading-info]
     [:div.is-flex.is-justify-content-space-between
      [:div.is-size-3.bold.mb-3 "Backup and Restore"]]
     [:p.my-3 "Use this hub to upload your files, manage translations, and export decks."]
     [:a.button.is-orange.bold
      {:on-click #(rf/dispatch [:fetch-backup])
       :class (when loading-backup :is-loading)}
      "Download Backup of Site"]
     [:p.my-3 "Or down here you can restore a site into Basha"]
     (r/with-let
       [draft_file (r/atom nil)]
       [:<>
        [:div.file.has-name
         [:label.file-label
          [:input.file-input {:type "file" :name "list" :on-change #(reset! draft_file (-> % .-target .-files (aget 0)))}]
          [:span.file-cta
           [:span.file-icon>i.fa.fa-upload]
           [:span.file-label "Choose a file"]]
          [:span.file-name (if @draft_file (.. @draft_file -name) [:i "No file selected"])]]]
        [:button.button.is-orange.bold.mt-5
         {:class (when loading-backup :is-loading)
          :on-click #(rf/dispatch [:restore {:file  @draft_file}])
          :disabled (nil? @draft_file)} "Restore"]])]))
