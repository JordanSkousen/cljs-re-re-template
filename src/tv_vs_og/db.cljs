(ns tv-vs-og.db
  (:require [re-frame.core :refer [reg-event-fx]]
            [tv-vs-og.api :as api]
            [tv-vs-og.data :refer [data]]))

(defn initial-db []
  {}) 

(defn initial-local-storage []
  {:albums (-> data keys vec)})

(reg-event-fx
 :initialize-db
 (fn [_ [_ local-storage]]
   (cond-> {:db (merge (initial-db)
                       {:local-storage (merge (initial-local-storage)
                                              local-storage)})}
     (and (:access-token local-storage)
          (not (:name local-storage))) 
     (assoc :dispatch [::api/get-user-profile]))))