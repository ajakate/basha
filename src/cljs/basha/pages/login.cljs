(ns basha.pages.login
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as r]))

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
        loading-info @(rf/subscribe [:loading-info])]
    (if loading-info
      [:div]
      (r/with-let [draft_user (r/atom nil)
                   draft_pass (r/atom nil)
                   show_pass (r/atom false)
                   is-signup (r/atom (is-first-user info))]
        [:div.px-6>div.basha-panel.mx-auto.is-flex.is-justify-content-center
         [:div.login-half.has-background-white.login-form.px-6.py-5
          (when (seq invite)
            [:div.mb-4
             [:p.is-italic "Login below first and then we'll take you back to your new list..."]])
          (if @is-signup
            [:<>
             [:h1.ftitle.bold "Welcome to Basha"]
             [:p.description "We're glad you decided to join our community. Let's make an account so you can get started."]]
            [:<>
             [:h1.ftitle.bold "Nice to See You Again"]
             [:p.description "Let's get signed back in so you can continue learning."]])
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
          (if @is-signup
            [:<>
             [:button.button.is-orange.bold.my-3
              {:on-click #(rf/dispatch [:signup {:username @draft_user :password @draft_pass}])
               :disabled (or (string/blank? @draft_user)
                             (string/blank? @draft_pass))} "Sign Up"]
             [:div [:span "Already have an account? "] [:a.link {:on-click #(swap! is-signup not)} "Log in"]]]
            [:<>
             [:button.button.is-orange.bold.my-3
              {:on-click #(rf/dispatch [:login {:username @draft_user :password @draft_pass}])
               :disabled (or (string/blank? @draft_user)
                             (string/blank? @draft_pass))} "Login"]
             (when (show-signup-option info invite)
               [:div [:span "Need an account? "] [:a.link {:on-click #(swap! is-signup not)} "Sign up"]])])]
         [:div.login-half.login-image
          [:img {:src "img/splash_v2.png"}]]]))))
