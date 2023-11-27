(ns tv-vs-og.views.game
  (:require [re-frame.core :refer [subscribe dispatch reg-event-db reg-event-fx reg-sub]]
            ["@mui/joy" :refer [Chip Card Typography Button Grid CircularProgress]]
            [tv-vs-og.api :as api]
            [tv-vs-og.data :refer [data]]))

(def TOTAL_ROUNDS 10)

(defn Album-Button
  [track]
  [:> Card {:sx {:display :flex
                 :flex-direction :column
                 :align-items :center}}
   [:img {:style {:max-width "45%"}
          :alt (:album-name track)
          :src (:image track)}]
   [:> Typography {:level :h1}
    (:name track)]
   [:> Typography {:component :i
                   :level :h4}
    (:album-name track)]])

(defn Game []
  (let [round-num @(subscribe [::round-num])
        {:keys [use]} @(subscribe [::round])
        og-track @(subscribe [::og-track])
        tv-track @(subscribe [::tv-track])
        round-loading? @(subscribe [::round-loading?])]
    [:<> 
     (when round-loading?
       [:> Card {:sx {:display :flex
                      :flex-direction :column
                      :align-items :center}}
        [:> CircularProgress]
        [:> Typography {:level :h2}
         "Loading Round " round-num "..."]])
     (when-not round-loading?
       [:div {:style {:text-align :center}}
        [:> Chip
         "ROUND " round-num "/" TOTAL_ROUNDS]
        [:> Typography {:level :h2}
         "What's playing?"]
        [:br]
        [:> Grid {:container true} 
         [:> Grid {:lg 5 :xs 12}
          [Album-Button og-track]]
         [:> Grid {:lg 2 :xs 12}
          [:span "OR"]]
         [:> Grid {:lg 5 :xs 12}
          [Album-Button tv-track]]]])]))


;; ================================================
;; Subscriptions 
(reg-sub
 ::round-num
 (fn [db]
   (:round db)))

(reg-sub
 ::round
 (fn [db]
   (get-in db [:rounds (dec (:round db))])))

(reg-sub
 ::round-loading?
 (fn [db]
   (:round-loading? db)))

(reg-sub
 ::og-track
 (fn [db]
   (:og-track db)))

(reg-sub
 ::tv-track
 (fn [db]
   (:tv-track db)))

;; ================================================
;; Handlers 
(reg-event-fx
 ::start
 (fn [{:keys [db]}]
   (let [chosen-albums (get-in db [:local-storage :albums])
         albums-data (->> data
                          seq
                          (filter (fn [[key]]
                                    (some #{key} chosen-albums)))
                          (map last)
                          (map (fn [{:keys [og-id tv-id tracks]}]
                                 (->> tracks
                                      seq
                                      (map (fn [[og-track-id tv-track-id]]
                                             {:og-id og-track-id
                                              :tv-id tv-track-id
                                              :og-album-id og-id
                                              :tv-album-id tv-id})))))
                          flatten)
         rounds (->> albums-data
                     shuffle
                     (take TOTAL_ROUNDS)
                     (mapv #(assoc % :use (if (even? (inc (rand-int 2))) :og :tv))))]
     {:db (merge db {:game-started? true
                     :round-loading? true
                     :round 1
                     :rounds rounds})
      :dispatch-n [[:close-alert]
                   [::load-round]]})))

(reg-event-fx
 ::load-round
 (fn [{:keys [db]}]
   (let [{:keys [use og-id tv-id]} (get-in db [:rounds (dec (:round db))])]
     {:async-flow {:first-dispatch [::api/get-track og-id]
                   :rules [{:when :seen?
                            :events (fn [[e response]] 
                                      (js/console.log {:e e :response response :id (:id response)})
                                      (and (= e :get-track-success)
                                           (= (:id response) og-id)))
                            :dispatch [::api/get-track tv-id]}
                           {:when :seen?
                            :events (fn [[e response]]
                                      (and (= e :get-track-success)
                                           (= (:id response) tv-id)))
                            :dispatch [::api/player-play {:track (if (= use :og) og-id tv-id)
                                                          :position 30}]}
                           {:when :seen?
                            :events [:player-play-success
                                     ::api/fail]
                            :halt? true}]}})))

(reg-event-db
 :get-track-success
 (fn [db [_ response]]
   (let [{:keys [og-id]} (get-in db [:rounds (dec (:round db))])]
     (assoc db 
            (if (= (:id response) og-id) :og-track :tv-track)
            (merge response
                   {:duration (/ (:duration_ms response) 1000)
                    :album-id (get-in response [:album :id])
                    :album-name (get-in response [:album :name])
                    :image (-> response
                               (get-in [:album :images])
                               first
                               :url)})))))

(reg-event-db
 :player-play-success
 (fn [db]
    (-> db 
       (assoc :round-loading? false)
       (assoc :player-playing? true))))