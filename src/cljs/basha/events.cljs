(ns basha.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [akiroz.re-frame.storage :refer [persist-db]]))

(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db :basha-app :user)]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))

;;dispatchers

(rf/reg-event-db
  :common/navigate
  (fn [db [_ match]]
    (let [old-match (:common/route db)
          new-match (assoc match :controllers
                                 (rfc/apply-controllers (:controllers old-match) match))]
      (assoc db :common/route new-match))))

(rf/reg-fx
  :common/navigate-fx!
  (fn [[k & [params query]]]
    (rfe/push-state k params query)))

(rf/reg-event-fx
  :common/navigate!
  (fn [_ [_ url-key params query]]
    {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(rf/reg-event-fx
  :fetch-docs
  (fn [_ _]
    {:http-xhrio {:method          :get
                  :uri             "/docs"
                  :response-format (ajax/raw-response-format)
                  :on-success       [:set-docs]}}))

(rf/reg-event-db
  :common/set-error
  (fn [db [_ error]]
    (assoc db :common/error error)))

(rf/reg-event-fx
  :page/init-home
  (fn [_ _]
    {:dispatch [:fetch-docs]}))

(rf/reg-event-fx
 :signup
 (fn [_ [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/api/signup"
                 :params user
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:login-redirect]
                 :on-failure [:set-signup-error]}}))

(rf/reg-event-fx
 :login
 (fn [_ [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/api/login"
                 :params user
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-login-user]
                 :on-failure [:set-signup-error]}}))

(defn generate-form-data [params]
  (let [form-data (js/FormData.)]
    (doseq [[k v] params]
      (.append form-data (name k) v))
    form-data))

(rf/reg-event-fx
 :create-list
 (fn [{:keys [db]} [_ params]]
   {:http-xhrio {:method          :post
                 :uri             "/api/lists"
                 :body (generate-form-data params)
                 :format          (ajax/json-request-format)
                 :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                ;;  :on-success       [:set-login-user]
                 :on-failure [:set-signup-error]}}))

;; (rf/reg-event-fx
;;  :refresh
;;  (fn [{:keys [db]} [_ params]]
;;    {:http-xhrio {:method          :post
;;                  :uri             "/api/refresh"
;;                  :params params
;;                  :format          (ajax/json-request-format)
;;                  :headers {"Authorization" (str "Token " (-> db :user :refresh-token))}
;;                  :response-format  (ajax/json-response-format {:keywords? true})
;;                  :on-success       [:set-login-user]
;;                  :on-failure [:set-signup-error]}}))

(rf/reg-event-db
 :swap-login-modal
 (fn [db [_ val]]
   (assoc db :login-modal/visible val)))

(rf/reg-event-db
 :close-login-modal
 (fn [db [_]]
   (assoc db :login-modal/visible false :login-modal/signup false :login-modal/errors nil)))

(rf/reg-event-db
 :set-signup-error
 (fn [db [_ response]]
   (assoc db :login-modal/errors (-> response :response :message))))

(rf/reg-event-db
 :set-signup
 (fn [db [_ val]]
   (assoc db :login-modal/signup val :login-modal/errors nil)))

(persisted-reg-event-db
 :set-login-user
 (fn [db [_ user]]
   (assoc db :user user :login-modal/visible false)))

(rf/reg-event-db
 :login-redirect
 (fn [db [_]]
   (assoc db :login-modal/visible true :login-modal/signup false :login-modal/errors nil)))

(persisted-reg-event-db
 :logout
 (fn [db [_]]
   (assoc db :user {})))

;;subscriptions

(rf/reg-sub
 :login-modal-visible
 (fn [db _]
   (-> db :login-modal/visible)))

(rf/reg-sub
 :is-signup
 (fn [db _]
   (-> db :login-modal/signup)))

(rf/reg-sub
 :login-errors
 (fn [db _]
   (-> db :login-modal/errors)))

(rf/reg-sub
 :user
 (fn [db _]
   (-> db :user)))

(rf/reg-sub
  :common/route
  (fn [db _]
    (-> db :common/route)))

(rf/reg-sub
  :common/page-id
  :<- [:common/route]
  (fn [route _]
    (-> route :data :name)))

(rf/reg-sub
  :common/page
  :<- [:common/route]
  (fn [route _]
    (-> route :data :view)))

(rf/reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(rf/reg-sub
  :common/error
  (fn [db _]
    (:common/error db)))
