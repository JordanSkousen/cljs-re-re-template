(ns cljs_re_re_template.db
  (:require [re-frame.core :refer [reg-event-fx]]))

(defn initial-db []
  {})

(reg-event-fx
 :initialize-db
 (fn [_ _]
   {:db (initial-db)}))