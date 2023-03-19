(ns cljs-re-re-template.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            ["@mui/material/styles" :refer [createTheme ThemeProvider]] 
            [devtools.core :as devtools]
            [cljs-re-re-template.views :as views]
            [cljs-re-re-template.db]
            [cljs-re-re-template.handlers]
            [cljs-re-re-template.subs]))

(defn install-devtools [] ; this is used to invert cljs console colors so it's actually readable in dark mode
  (let [{:keys [cljs-land-style]} (devtools/get-prefs)]
    (devtools/set-pref! :cljs-land-style (str "filter:invert(1);" cljs-land-style)))
  (devtools/install!))

(def mui-theme (createTheme (clj->js {:palette {:primary {:main "#000"}
                                                :secondary {:main "#fff"}}
                                      :typography {:fontFamily "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif"}})))

(defn ^:dev/after-load mount-root []
  (rdom/render [:> ThemeProvider {:theme mui-theme}
                [views/Main]] 
               (.getElementById js/document "app")))

(defn init []
  (rf/dispatch-sync [:initialize-db])
  (install-devtools)
  (mount-root))