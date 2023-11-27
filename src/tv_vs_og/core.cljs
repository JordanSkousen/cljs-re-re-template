(ns tv-vs-og.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [devtools.core :as devtools]
            [day8.re-frame.async-flow-fx]
            [tv-vs-og.views :as views]
            [tv-vs-og.db] 
            [tv-vs-og.handlers]
            [tv-vs-og.http]
            [tv-vs-og.subs]
            [tv-vs-og.views.game]))

(defn install-devtools [] ; this is used to invert cljs console colors so it's actually readable in dark mode
  (let [{:keys [cljs-land-style]} (devtools/get-prefs)]
    (devtools/set-pref! :cljs-land-style (str "filter:invert(1);" cljs-land-style)))
  (devtools/install!))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (rdom/render [views/Main]
   (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db (js->clj (.parse js/JSON (.getItem js/window.localStorage "db")) :keywordize-keys true)])
  (install-devtools)
  (mount-root))