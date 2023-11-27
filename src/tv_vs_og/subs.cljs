(ns tv-vs-og.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 ::local-storage
 (fn [db]
   (:local-storage db)))

(reg-sub
 :logged-in?
 :<- [::local-storage]
 (fn [ls]
   (some? (:access-token ls))))

(reg-sub
 :login-loading?
 (fn [db]
   (:login-loading? db)))

(reg-sub
 :user-name
 (fn [db]
   (get-in db [:user :name])))

(reg-sub
 :user-image
 (fn [db]
   (get-in db [:user :image])))

(reg-sub
 :albums
 :<- [::local-storage]
 (fn [ls]
   (:albums ls)))

(reg-sub
 :alert
 (fn [db]
   (:alert db)))

(reg-sub
 :game-started?
 (fn [db]
   (:game-started? db)))