(ns basha.pages.login
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as r]))

;; TODOO: deprecate this
;; add message about what todo if forgot account
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

(defn login-page []
  (let [error @(rf/subscribe [:login-errors])]
    (r/with-let [draft_user (r/atom nil)
                 draft_pass (r/atom nil)
                 show_pass (r/atom false)]
      [:div.basha-panel.is-flex.is-justify-content-center
       [:div.login-half.has-background-white.login-form.px-6.py-5
        [:h1.ftitle.bold "Nice to See You Again"]
        [:p.description "Let's get signed back in so you can continue learning."]
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
        [:button.button.is-orange.bold.mt-3
         {:on-click #(rf/dispatch [:login {:username @draft_user :password @draft_pass}])
          :disabled (or (string/blank? @draft_user)
                        (string/blank? @draft_pass))} "Login"]
        [:button.button.is-orange.bold.mt-3.mx-2
         {:on-click #(rf/dispatch [:signup {:username @draft_user :password @draft_pass}])
          :disabled (or (string/blank? @draft_user)
                        (string/blank? @draft_pass))} "Create Account TODOO: deleteme"]]
       [:div.login-half.login-image
        [:img {:src "img/splash_v2.png"}]]])))
