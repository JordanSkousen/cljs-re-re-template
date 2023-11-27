(ns tv-vs-og.handlers
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx]]
            [tv-vs-og.api :as api]
            [tv-vs-og.views.game :as game]))

(reg-event-fx
 :toggle-album
 (fn [{:keys [db]} [_ name]]
  (if (some #{name} (get-in db [:local-storage :albums]))
    {:dispatch [::update-local-storage assoc :albums (->> (get-in db [:local-storage :albums])
                                                          (filter (fn [n] (not= n name))))]}
    {:dispatch [::update-local-storage update :albums conj name]})))

(reg-event-fx
 :show-info-alert
 (fn [{:keys [db]}]
   (if-not (get-in db [:local-storage :info-alert-shown])
     {:db (assoc db :alert {:title "Your Spotify will now play"
                            :message [:<>
                                      [:p "This app will use your Spotify account to play the songs, so make sure:"]
                                      [:ol
                                       [:li "The Spotify app is open"]
                                       [:li "Your speakers are turned up"]
                                       [:li "No peeking at what's currently playing! That's cheating 😉"]]]
                            :buttons [{:label "Let's Play!"
                                       :on-click [::game/start]}
                                      {:label "Open Spotify"
                                       :href "https://open.spotify.com"
                                       :color :neutral}
                                      {:label "Go Back"
                                       :variant :outlined}]})}
     {:dispatch [::game/start]})))

(reg-event-db
 :close-alert
 (fn [db]
   ( dissoc db :alert)))

(reg-event-fx
 :login
 (fn [{:keys [db]} [_ code]]
   {:db (assoc db :login-loading? true)
    :async-flow {:first-dispatch [::api/exchange-login-code code]
                 :rules [{:when :seen?
                          :events [:exchange-login-code-success
                                   ::update-local-storage]
                          :dispatch [::api/get-user-profile]}
                         {:when :seen?
                          :events [:get-user-profile-success
                                   ::api/fail]
                          :halt? true}]}
    ::push-history-state "/"}))

(reg-event-fx
 :exchange-login-code-success
 (fn [_ [_ response]]
   (if-let [access-token (:access_token response)]
     {:dispatch [::update-local-storage merge {:access-token access-token
                                               :refresh-token (:refresh_token response)}]}
     {:dispatch [::api/fail response]})))

(reg-event-fx
 :refresh-access-token-success
 (fn [_ [_ dispatch-coll response]]
   {:async-flow {:first-dispatch [::update-local-storage merge {:access-token (:access_token response)
                                                                :refresh-token (:refresh_token response)}]
                 :rules [{:when :seen?
                          :events [::update-local-storage]
                          :dispatch dispatch-coll
                          :halt? true}]}}))

(reg-event-db
 :get-user-profile-success
 (fn [db [_ response]]
   (assoc db :user {:name (:display_name response)
                    :image (-> response :images first :url)})))

(reg-event-db
 :error
 (fn [db [_ title message]]
   (assoc db :alert {:title title 
                     :message message})))

(reg-event-fx
 ::update-local-storage
 (fn [{:keys [db]} [_ & args]]
   (let [db' (apply update db :local-storage args)]
     {:db db'
      ::do-write-local-storage (:local-storage db')})))

(reg-fx
 ::push-history-state
 (fn [url]
   (.pushState js/window.history nil (.-title js/document) url)))

(reg-fx
 ::do-write-local-storage
 (fn [local-storage]
   (.setItem js/window.localStorage "db" (.stringify js/JSON (clj->js local-storage)))))