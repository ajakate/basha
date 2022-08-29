(ns basha.pages.invite
  (:require
   [re-frame.core :as rf]
   [clojure.string :as string]))

(defn already-member [user invite]
  (let [user_id (:id user)
        owner_id (:user_id invite)
        existing_users (:users invite)]
    (if (= user_id owner_id)
      true
      (if (empty? existing_users)
        false
        (some #{user_id} (string/split existing_users #","))))))

(defn invite-page []
  (let [invite @(rf/subscribe [:invite])
        user @(rf/subscribe [:user])]
    [:div.px-6>div.basha-panel.mx-auto.is-flex.is-justify-content-center
     (if (already-member user invite)
       [:div.login-half.has-background-white.login-form.px-6.py-5
        [:h1.is-size-3.mb-4 "You're all set!"]
        [:div.mb-4 [:span "You already have access to "] [:span.bold (:name invite)]]
        [:button.button.is-orange.mt-4
         {:on-click #(rf/dispatch [:redirect-home])}
         "Go to Dashboard"]]
       [:div.login-half.has-background-white.login-form.px-6.py-5
        [:h1.is-size-3.mb-4 "You've been invited!"]
        [:div.mb-4 [:span.bold (:username invite)] [:span " has invited you to join "] [:span.bold (:name invite)]]
        [:p.mb-4 "Once you accept, we'll take you back to your dashboard"]
        [:button.button.is-orange.mt-4
         {:on-click #(rf/dispatch [:add-share {:user_id (:id user) :list_id (:id invite)}])}
         "Accept"]])
     [:div.login-half.login-image
      [:img {:src "img/splash_v2.png"}]]]))
