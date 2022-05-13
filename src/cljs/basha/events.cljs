(ns basha.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [ajax.protocols :as protocols]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [basha.audio :as baudio]
   [akiroz.re-frame.storage :refer [persist-db-keys]]))

(def with-auth
  (rf/->interceptor
   :id      :with-auth
   :after (fn [context]
            (let [token (-> context :coeffects :db :user :access-token)
                  original (-> context :coeffects :event)
                  failure (-> context :effects :http-xhrio :on-failure)
                  with-fail (assoc-in context [:effects :http-xhrio :on-failure] [:with-auth-failure original failure])]
              (assoc-in with-fail [:effects :http-xhrio :headers] {"Authorization" (str "Token " token)})))))

(defn with-loading-state [state-var]
  (rf/->interceptor
   :id :loading-state
   :after (fn [context]
            (let [original (-> context :coeffects :event)
                  failure (-> context :effects :http-xhrio :on-failure)
                  success (-> context :effects :http-xhrio :on-success)
                  with-fail (assoc-in context [:effects :http-xhrio :on-failure] [:with-loading-false state-var failure])
                  with-loading (assoc-in with-fail [:effects :http-xhrio :on-request] [:set-loading state-var original])]
              (assoc-in with-loading [:effects :http-xhrio :on-success] [:with-loading-false state-var success])))))

(rf/reg-event-db
 :set-loading
 (fn [db [_ state-var event _]]
   (assoc db state-var event)))

(rf/reg-event-fx
 :with-loading-false
 (fn [{:keys [db]} [_ state-var orig resp]]
   {:db (assoc db state-var nil)
    :fx [[:dispatch (conj orig resp)]]}))

(rf/reg-event-fx
 :with-auth-failure
 (fn [_ [_ original failure resp]]
   (if (= (:status resp) 401)
     {:fx [[:dispatch [:refresh original]]]}
     {:fx [[:dispatch (conj failure resp)]]})))

(defn persisted-reg-event-db
  [event-id handler]
  (rf/reg-event-fx
   event-id
   [(persist-db-keys :basha-app [:user :hide-native])]
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

(defn download-file!
  [data content-type file-name]
  (let [data-blob (js/Blob. #js [data] #js {:type content-type})
        link (js/document.createElement "a")]
    (set! (.-href link) (js/URL.createObjectURL data-blob))
    (.setAttribute link "download" file-name)
    (js/document.body.appendChild link)
    (.click link)
    (js/document.body.removeChild link)))

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
     {:dispatch [:fetch-list-summary]}
     {:dispatch [:fetch-docs]})))

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

(rf/reg-event-fx
 :create-list
 [(with-loading-state :loading-create-list) with-auth]
 (fn [_ [_ params]]
   {:http-xhrio {:method          :post
                 :uri             "/api/lists"
                 :body (generate-form-data params)
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:redirect-home]
                 :on-failure [:set-create-list-error]}}))

(rf/reg-event-fx
 :export-list
 [with-auth]
 (fn [_ [_ id name]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/decks/" id)
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:fetch-deck id name]
                 :on-failure [:kill-download-modal]}}))

(rf/reg-event-fx
 :fetch-deck
 [with-auth]
 (fn [_ [_ id name _]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/decks/" id)
                 :response-format  {:description "file-download"
                                    :content-type "*/*"
                                    :type :blob
                                    :read protocols/-body}
                 :on-success       [:download-file name]
                 :on-failure [:handle-deck-failure id name]}}))

(rf/reg-event-fx
 :delete-audio
 [with-auth]
 (fn [_ [_ id]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/delete_audio/" id)
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:reload-translation]}}))

(rf/reg-event-fx
 :edit-translation
 [with-auth]
 (fn [_ [_ params]]
   (let [id (:id params)
         body (dissoc params :id :list_id :goto-next :next_id)
         list_id (:list_id params)
         next-id (:next_id params)]
     {:http-xhrio {:method          :post
                   :uri             (str "/api/translations/" id)
                   :body (generate-form-data body)
                   :format          (ajax/json-request-format)
                   :response-format  (ajax/json-response-format {:keywords? true})
                   :on-success       (if next-id [:fetch-translation next-id] [:reset-list-page list_id])}})))

(rf/reg-event-fx
 :fetch-list-summary
 [with-auth]
 (fn [_ [_ _]]
   {:http-xhrio {:method          :get
                 :uri             "/api/lists"
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-list-summary]
                 :on-failure [:logout]}}))

(rf/reg-event-fx
 :fetch-translation
 [(with-loading-state :loading-translation) with-auth]
 (fn [_ [_ id]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/translations/" id)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-active-translation]
                 :on-failure [:set-signup-error]}}))

(rf/reg-event-fx
 :fetch-list
 [(with-loading-state :loading-list) with-auth]
 (fn [_ [_ id]]
   {:http-xhrio {:method          :get
                 :uri             (str "/api/lists/" id)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-active-list]}}))

(rf/reg-event-fx
 :edit-users
 [with-auth]
 (fn [_ [_ params]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/assignees/" (:list_id params))
                 :params {:users (:users params)}
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:reset-list-page (:list_id params)]
                 :on-failure [:set-users-error]}}))

(rf/reg-event-fx
 :delete-list
 [with-auth]
 (fn [_ [_ id]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/delete_list")
                 :params {:id id}
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:clear-list-delete]}}))

(rf/reg-event-fx
 :delete-translation
 [with-auth]
 (fn [_ [_ id]]
   {:http-xhrio {:method          :post
                 :uri             (str "/api/delete_translation")
                 :params {:id id}
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:clear-translation-delete]}}))

(rf/reg-event-fx
 :refresh
 (fn [{:keys [db]} [_ original]]
   {:http-xhrio {:method          :post
                 :uri             "/api/refresh"
                 :format          (ajax/json-request-format)
                 :headers {"Authorization" (str "Token " (-> db :user :refresh-token))}
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:refresh-success original]
                 :on-failure [:logout]}}))

(rf/reg-event-fx
 :refresh-success
 (fn [_ [_ original resp]]
   {:fx [[:dispatch [:set-login-user resp]] [:dispatch original]]}))

(rf/reg-event-db
 :set-media-error
 (fn [db [_ message]]
   (assoc db :media-error message)))

(rf/reg-event-db
 :set-delete-list-id
 (fn [db [_ id]]
   (assoc db :delete-list-id id)))

(rf/reg-event-db
 :set-delete-translation-id
 (fn [db [_ id]]
   (assoc db :delete-translation-id id)))

(rf/reg-event-db
 :clear-delete-list-id
 (fn [db [_]]
   (assoc db :delete-list-id nil)))

(rf/reg-event-db
 :clear-delete-translation-id
 (fn [db [_]]
   (assoc db :delete-translation-id nil)))

(rf/reg-event-fx
 :clear-list-delete
 (fn [_ [_]]
   {:fx [[:dispatch [:clear-delete-list-id]] [:dispatch [:redirect-home]]]}))

(rf/reg-event-fx
 :clear-translation-delete
 (fn [{:keys [db]} [_]]
   {:fx [[:dispatch [:clear-delete-translation-id]] [:dispatch [:load-list-page (-> db :active-list :id)]]]}))

(rf/reg-event-db
 :set-users-error
 (fn [db [_ resp]]
   (assoc db :users-error (-> resp :response :message))))

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
   {:dispatch [:fetch-list id]}))

(rf/reg-event-db
 :reload-translation
 (fn [db [_]]
   (assoc-in db [:active-translation :audio] nil)))

(rf/reg-event-fx
 :download-file
 (fn [_ [_ name resp]]
   (download-file! resp "application/apkg" (str "Basha - " name ".apkg"))
   {:dispatch [:kill-download-modal]}))

(rf/reg-event-fx
 :set-downloading-deck
 (fn [{:keys [db]} [_ id name]]
   {:db  (assoc db :is-downloading true)
    :dispatch [:export-list id name]}))

(rf/reg-event-fx
 :handle-deck-failure
 (fn [_ [_ id name resp]]
   (if (= 404 (:status resp))
     (do
       (js/setTimeout (fn []) 1000)
       {:dispatch [:fetch-deck id name]})
     {:dispatch [:set-download-error resp]})))

; TODO: what's wrong with resp/json
(rf/reg-event-db
 :set-download-error
 (fn [db [_ resp]]
   (assoc db :download-error "Sorry, appears as though something bad happened... please try again.")))

(rf/reg-event-db
 :kill-download-modal
 (fn [db [_]]
   (assoc db :is-downloading false :download-error nil)))

(rf/reg-event-fx
 :reset-list-page
 (fn [{:keys [db]} [_ list_id _]]
   {:db (assoc db
               :translate-modal/visible false
               :users-modal-visible false
               :users-error nil
               :active-translation nil)
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
   {:db (assoc db :recording-state :armed :temp-recording nil)
    :dispatch [:stop-media-recorder]}))

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
   (assoc db :create-list-error true)))

(rf/reg-event-db
 :clear-create-list-error
 (fn [db [_]]
   (assoc db :create-list-error nil)))

(rf/reg-event-db
 :set-active-list
 (fn [db [_ response]]
   (assoc db :active-list response)))

(rf/reg-event-db
 :open-login-modal
 (fn [db [_]]
   (assoc db :login-modal/visible true)))

(rf/reg-event-db
 :close-login-modal
 (fn [db [_]]
   (assoc db :login-modal/visible false :login-modal/signup false :login-modal/errors nil)))

(rf/reg-event-fx
 :close-translate-modal
 (fn [{:keys [db]} _]
   {:db (assoc db :translate-modal/visible false :active-translation nil)
    :fx  [[:dispatch [:cancel-recording]] [:dispatch [:load-list-page (-> db :active-list :id)]]]}))

(rf/reg-event-fx
 :set-active-translation
 (fn [{:keys [db]} [_ response]]
   (let [id (:id response)
         list (:active-list db)
         next-id (next-id-in-list list id)]
     {:db (assoc db
                 :active-translation (assoc response :next_id next-id)
                 :translate-modal/visible true)
      :dispatch [:arm-recording]})))

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
   (assoc db :users-modal-visible false :users-error nil)))

(persisted-reg-event-db
 :set-login-user
 (fn [db [_ user]]
   (assoc db :user user :login-modal/visible false)))

(persisted-reg-event-db
 :swap-hide-native
 (fn [db [_]]
   (assoc db :hide-native (not (:hide-native db)))))

(rf/reg-event-fx
 :set-login
 (fn [_ [_ user]]
   {:fx [[:dispatch [:close-login-modal]] [:dispatch [:set-login-user user]] [:dispatch [:redirect-home]]]}))

(persisted-reg-event-db
 :clear-login-user
 (fn [db [_]]
   (assoc db :user {})))

(rf/reg-event-fx
 :logout
 (fn [_ [_]]
   {:db {}
    :fx [[:dispatch [:clear-login-user]] [:dispatch [:redirect-home]]]}))

;;subscriptions

(rf/reg-sub
 :is-downloading
 (fn [db _]
   (-> db :is-downloading)))

(rf/reg-sub
 :download-error
 (fn [db _]
   (-> db :download-error)))

(rf/reg-sub
 :delete-list-id
 (fn [db _]
   (-> db :delete-list-id)))

(rf/reg-sub
 :delete-translation-id
 (fn [db _]
   (-> db :delete-translation-id)))

(rf/reg-sub
 :media-error
 (fn [db _]
   (-> db :media-error)))

(rf/reg-sub
 :hide-native
 (fn [db _]
   (-> db :hide-native)))

(rf/reg-sub
 :loading-create-list
 (fn [db _]
   (-> db :loading-create-list)))

(rf/reg-sub
 :loading-list
 (fn [db _]
   (-> db :loading-list)))

(rf/reg-sub
 :users-error
 (fn [db _]
   (-> db :users-error)))

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
