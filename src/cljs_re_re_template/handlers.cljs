(ns cljs-re-re-template.handlers
  (:require [re-re-frame.core :refer [reg-event-x reg-fx grab]]))

(defn initial-db []
  {})

(reg-event-x
 ::initialize-db
 (fn []
   {:db (initial-db)}))