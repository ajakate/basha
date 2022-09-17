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
     (if (= (:total_users info) 0)
       (r/with-let
         [draft_file (r/atom nil)]
         [:<>
          [:p.my-3 "If you have an archived Basha site, you can use the form below to upload it here and restore all your data from a previous Basha installation."]
          [:div.my-3.mb-5
           [:span "Click "]
           [:a.link {:href "https://www.bashalang.org/guide.html#restore" :target "_blank"} "here"]
           [:span " for more info."]]
          [:div.file.has-name.mb-3
           [:label.file-label
            [:input.file-input {:type "file" :name "list" :on-change #(reset! draft_file (-> % .-target .-files (aget 0)))}]
            [:span.file-cta
             [:span.file-icon>i.fa.fa-upload]
             [:span.file-label "Choose a file"]]
            [:span.file-name (if @draft_file (.. @draft_file -name) [:i "No file selected"])]]]
          [:button.button.is-orange.bold.mt-5
           {:class (when loading-backup :is-loading)
            :on-click #(rf/dispatch [:restore {:file  @draft_file}])
            :disabled (nil? @draft_file)} "Restore"]])
       [:<>
        [:p.my-5.mb-4
         [:div "Download an archive of your site here. "
          "Your entire site will be saved in this archive, including users, lists, and translations."]
         [:div.mt-3
          [:span "Click "]
          [:a.link {:href "https://www.bashalang.org/guide.html#restore" :target "_blank"} "here"]
          [:span " for more info."]]]
        [:a.button.is-orange.bold
         {:on-click #(rf/dispatch [:fetch-backup])
          :class (when loading-backup :is-loading)}
         "Download Backup of Site"]])]))
