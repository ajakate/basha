(ns basha.pages.login
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [basha.components.shared :refer [loading-screen]]))

(defn is-first-user [info]
  (= 0 (:total_users info)))

(defn show-signup-option [info invite]
  (if (seq invite)
    true
    (is-first-user info)))

(defn login-page []
  (let [error @(rf/subscribe [:login-errors])
        invite @(rf/subscribe [:invite])
        info @(rf/subscribe [:info])
        loading @(rf/subscribe [:loading-login])
        is-signup @(rf/subscribe [:is-signup])]
    (r/with-let [draft_user (r/atom nil)
                 draft_pass (r/atom nil)
                 show_pass (r/atom false)]
      [:div.px-6>div.basha-panel.mx-auto.is-flex.is-justify-content-center
       [loading-screen :loading-info]
       [:div.login-half.has-background-white.login-form.px-6.py-5
        (when (seq invite)
          [:div.mb-4
           [:p.is-italic "Login below first and then we'll take you back to your new list..."]])
        (if is-signup
          [:<>
           [:h1.is-size-5.mb-4.bold "Welcome to Basha"]
           [:p.mb-4 "We're glad you decided to join our community. Let's make an account so you can get started."]]
          [:<>
           [:h1.is-size-5.mb-4.bold "Nice to See You Again"]
           [:p.mb-4 "Let's get signed back in so you can continue learning."]])
        [:p.bold "Username"]
        [:input.finput
         {:type "text"
          :placeholder "sk8hkr69"
          :on-change #(reset! draft_user (.. % -target -value))
          :value @draft_user}]
        [:p.bold "Password"]
        [:div [:input.finput
               {:type (if @show_pass "text" "password")
                :placeholder "test123"
                :on-change #(reset! draft_pass (.. % -target -value))
                :value @draft_pass}]
         [:i.fa {:class (if @show_pass :fa-eye-slash :fa-eye) :on-click #(swap! show_pass not)}]]
        (when error [:p.has-text-danger.is-italic.mb-3 (str "Error: " error)])
        (if is-signup
          [:<>
           [:button.button.is-orange.bold.my-3
            {:on-click #(rf/dispatch [:signup {:username @draft_user :password @draft_pass}])
             :disabled (or (string/blank? @draft_user)
                           (string/blank? @draft_pass))
             :class (when loading :is-loading)} "Sign Up"]
           [:div [:span "Already have an account? "] [:a.link {:on-click #(rf/dispatch [:is-signup false])} "Log in"]]]
          [:<>
           [:button.button.is-orange.bold.my-3
            {:on-click #(rf/dispatch [:login {:username @draft_user :password @draft_pass}])
             :disabled (or (string/blank? @draft_user)
                           (string/blank? @draft_pass))
             :class (when loading :is-loading)} "Login"]
           (when (show-signup-option info invite)
             [:div [:span "Need an account? "] [:a.link {:on-click #(rf/dispatch [:is-signup true])} "Sign up"]])
           ])
        (when (= 0 (:total_users info))
          [:div.bold.mt-5
           "You can also "
           [:a {:href "/#/backup"} "click here"] " to restore an archived Basha site."])]
       [:div.login-half.login-image.is-hidden-mobile
        [:img {:src "img/splash_v2.png"}]]])))
