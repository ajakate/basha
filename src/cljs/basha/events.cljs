(ns basha.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [basha.audio :as baudio]
   [akiroz.re-frame.storage :refer [persist-db]]))

(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db :basha-app :user)]
   (fn [{:keys [db]} event-vec]
     {:db (handler db event-vec)})))

(persisted-reg-event-db :init-local-storage (fn [db] db))

(defn position [x coll & {:keys [from-end all] :or {from-end false all false}}]
  (let [all-idxs (keep-indexed (fn [idx val] (when (= val x) idx)) coll)]
    (cond
      (true? from-end) (last all-idxs)
      (true? all)      all-idxs
      :else            (first all-idxs))))

(defn next-id-in-list [list id]
  (let [ids (map #(:id %) (:translations list))
        i (position id ids)]
    (second (nthnext ids i))))

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
  (fn [{:keys [db]} _]
    (if (seq (:user db))
      (rf/dispatch [:fetch-list-summary]))
    {:dispatch [:fetch-docs]}))

(rf/reg-event-fx
 :signup
 (fn [_ [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/api/signup"
                 :params user
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-login]
                 :on-failure [:set-signup-error]}}))

(rf/reg-event-fx
 :login
 (fn [_ [_ user]]
   {:http-xhrio {:method          :post
                 :uri             "/api/login"
                 :params user
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-login]
                 :on-failure [:set-signup-error]}}))

(defn filter-empty [params]
  (into {} (filter (fn [kv]
                     (let [v (second kv)]
                       (and
                        (some? v)
                        (not= v "")))) params)))

(defn generate-form-data [params]
  (let [form-data (js/FormData.)
        clean-params (filter-empty params)]
    (doseq [[k v] clean-params]
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
                 :on-success       [:redirect-home]
                 :on-failure [:set-create-list-error]}}))

(rf/reg-event-fx
 :edit-translation
 (fn [{:keys [db]} [_ params]]
   (let [id (:id params)
         body (dissoc params :id :list_id :goto-next :next_id)
         list_id (:list_id params)
         next-id (:next_id params)]
     {:http-xhrio {:method          :post
                   :uri             (str "/api/translations/" id)
                   :body (generate-form-data body)
                   :format          (ajax/json-request-format)
                   :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                   :response-format  (ajax/json-response-format {:keywords? true})
                   :on-success       (if next-id [:open-translate-modal next-id] [:reset-list-page list_id])
                   :on-failure [:set-signup-error]}})))

(rf/reg-event-fx
 :fetch-list-summary
 (fn [{:keys [db]} [_ _]]
   {:http-xhrio {:method          :get
                 :uri             "/api/lists"
                 :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-list-summary]
                 :on-failure [:set-list-summary]}}))

(rf/reg-event-fx
 :fetch-translation
 (fn [{:keys [db]} [_ id]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/translations/" id)
                 :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-active-translation]
                ;;  :on-failure [:set-list-summary]
                 }}))

(rf/reg-event-fx
 :fetch-list
 (fn [{:keys [db]} [_ id]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/lists/" id)
                 :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-active-list]
                ;;  :on-failure [:set-list-summary]
                 }}))

(rf/reg-event-fx
 :edit-users
 (fn [{:keys [db]} [_ params]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/assignees/" (:list_id params))
                 :params {:users (:users params)}
                 :format          (ajax/json-request-format)
                 :headers {"Authorization" (str "Token " (-> db :user :access-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:reset-list-page (:list_id params)]
                 :on-failure [:set-create-list-error]}}))

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

; TODO: IMPLEMENT THIS FOR REDIRECT
;; (rf/reg-event-fx
;;  :set-track-url
;;  (fn [_ [_ track]]
;;    (rfe/push-state :view-track {:id (:id track)})))

(rf/reg-event-fx
 :set-home-state
 (fn [_ [_]]
   (rfe/push-state :home)))

(rf/reg-event-fx
 :redirect-home
 (fn [{:keys [_]} [_]]
   {:fx [[:dispatch [:set-home-state]] [:dispatch [:page/init-home]]]}))

(rf/reg-event-fx
 :load-list-page
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :loading-list true)
    :dispatch [:fetch-list id]}))

(rf/reg-event-fx
 :reset-list-page
 (fn [{:keys [db]} [_ list_id _]]
   {:db (assoc db :translate-modal/visible false :users-modal-visible false)
    :dispatch [:load-list-page list_id]}))

(rf/reg-event-fx
 :init-media-recorder
 (fn [_ [_]]
   (baudio/init-audio)))

(rf/reg-event-fx
 :start-media-recorder
 (fn [_ [_]]
   (baudio/start-audio)))

(rf/reg-event-fx
 :stop-media-recorder
 (fn [_ [_]]
   (baudio/stop-audio)))

(rf/reg-event-db
 :set-temp-recording
 (fn [db [_ blob]]
   (assoc db :temp-recording blob)))

(rf/reg-event-fx
 :arm-recording
 (fn [{:keys [db]} [_]]
   {:db (assoc db :recording-state :armed)
    :dispatch [:init-media-recorder]}))

(rf/reg-event-fx
 :start-recording
 (fn [{:keys [db]} [_]]
   {:db (assoc db :recording-state :recording)
    :dispatch [:start-media-recorder]}))

(rf/reg-event-fx
 :stop-recording
 (fn [{:keys [db]} [_]]
   {:db (assoc db :recording-state :stopped)
    :dispatch [:stop-media-recorder]}))

(rf/reg-event-fx
 :cancel-recording
 (fn [{:keys [db]} [_]]
   {:db (assoc db :recording-state :init :temp-recording nil)
    :dispatch [:stop-media-recorder]}))

;; TODO: keep this for now
(rf/reg-event-db
 :debug
 (fn [db [_ obj]]
   (assoc db :debug obj)))

(rf/reg-event-db
 :set-list-summary
 (fn [db [_ response]]
   (assoc db :list-summary response)))

(rf/reg-event-db
 :set-create-list-error
 (fn [db [_ response]]
   (assoc db :create-list-error (-> response :response :message))))

(rf/reg-event-db
 :clear-create-list-error
 (fn [db [_]]
   (assoc db :create-list-error nil)))

(rf/reg-event-db
 :set-active-list
 (fn [db [_ response]]
   (assoc db :active-list response :loading-list false)))

(rf/reg-event-db
 :swap-login-modal
 (fn [db [_ val]]
   (assoc db :login-modal/visible val)))

(rf/reg-event-db
 :close-login-modal
 (fn [db [_]]
   (assoc db :login-modal/visible false :login-modal/signup false :login-modal/errors nil)))

(rf/reg-event-fx
 :open-translate-modal
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :translate-modal/visible true :loading-translation true :recording-state :init)
    :dispatch [:fetch-translation id]}))

(rf/reg-event-db
 :close-translate-modal
 (fn [db [_]]
   (assoc db :translate-modal/visible false :loading-translation false :active-translation nil)))

(rf/reg-event-fx
 :close-translate-modal
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :translate-modal/visible false :loading-translation false :active-translation nil)
    :dispatch [:cancel-recording]}))

(rf/reg-event-db
 :set-active-translation
 (fn [db [_ response]]
   (let [id (:id response)
         list (:active-list db)
         next-id (next-id-in-list list id)]
     (assoc db :active-translation (assoc response :next_id next-id) :loading-translation false))))

(rf/reg-event-db
 :set-signup-error
 (fn [db [_ response]]
   (assoc db :login-modal/errors (-> response :response :message))))

(rf/reg-event-db
 :set-signup
 (fn [db [_ val]]
   (assoc db :login-modal/signup val :login-modal/errors nil)))

(rf/reg-event-db
 :open-users-modal
 (fn [db [_]]
   (assoc db :users-modal-visible true)))

(rf/reg-event-db
 :close-users-modal
 (fn [db [_]]
   (assoc db :users-modal-visible false)))

(persisted-reg-event-db
 :set-login-user
 (fn [db [_ user]]
   (assoc db :user user :login-modal/visible false)))

(rf/reg-event-fx
 :set-login
 (fn [{:keys [db]} [_ user]]
   {:fx [[:dispatch [:set-login-user user]] [:dispatch [:redirect-home]]]}))

(persisted-reg-event-db
 :clear-login-user
 (fn [db [_]]
   (assoc db :user {})))

; TODO: clear stuff here?
(rf/reg-event-fx
 :logout
 (fn [{:keys [db]} [_]]
   {:fx [[:dispatch [:clear-login-user]] [:dispatch [:redirect-home]]]}))

;;subscriptions

(rf/reg-sub
 :users-modal-visible
 (fn [db _]
   (-> db :users-modal-visible)))

(rf/reg-sub
 :users
 (fn [db _]
   (-> db :users)))

(rf/reg-sub
 :temp-recording
 (fn [db _]
   (-> db :temp-recording)))

(rf/reg-sub
 :recording-state
 (fn [db _]
   (-> db :recording-state)))

(rf/reg-sub
 :create-list-error
 (fn [db _]
   (-> db :create-list-error)))

(rf/reg-sub
 :active-list
 (fn [db _]
   (-> db :active-list))) 

(rf/reg-sub
 :loading-translation
 (fn [db _]
   (-> db :loading-translation)))

(rf/reg-sub
 :active-translation
 (fn [db _]
   (-> db :active-translation)))

(rf/reg-sub
 :list-summary
 (fn [db _]
   (-> db :list-summary)))

(rf/reg-sub
 :login-modal-visible
 (fn [db _]
   (-> db :login-modal/visible)))

(rf/reg-sub
 :translate-modal-visible
 (fn [db _]
   (-> db :translate-modal/visible)))

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
