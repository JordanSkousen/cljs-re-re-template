(ns tv-vs-og.views
  (:require [clojure.set :refer [rename-keys]]
            [reagent.core :as r]
            [re-frame.core :refer [subscribe dispatch]]
            ["@mui/joy" :refer [Card Typography Button Avatar Stack Switch CircularProgress Modal ModalDialog ModalClose DialogTitle DialogContent DialogActions]]
            [tv-vs-og.data :refer [data]]
            [tv-vs-og.views.game :refer [Game]]))

(defn Start []
  (let [logged-in? @(subscribe [:logged-in?])
        login-loading? @(subscribe [:login-loading?])
        albums @(subscribe [:albums])]
    [:> Card
     (when login-loading?
       [:> CircularProgress])
     (when (and logged-in? (not login-loading?))
       [:div {:style {:width "100%"
                      :display :flex
                      :align-items :center}} 
        [:> Avatar {:src @(subscribe [:user-image])}]
        [:> Typography {:sx {:margin-left "0.7em"}}
         "Logged in as "
         [:b @(subscribe [:user-name])]]])
     [:> Typography {:level :h1
                     :color :primary}
      "Taylor's Version vs. Original"]
     [:> Typography {:level :h3}
      "Can you guess which version is playing?"] 
     (when logged-in?
       [:> Stack
        [:> Typography "Include Albums:"]
        (doall
         (for [album (keys data)]
           ^{:key album}
           [:> Typography {:component :label
                           :startDecorator (r/as-element [:> Switch {:checked (some #{album} albums)
                                                                     :on-change #(when-not (and (some #{album} albums) (= (count albums) 1))
                                                                                   (dispatch [:toggle-album album]))}])}
            album]))])
     [:> Button (cond-> {:color (if logged-in? :primary :neutral)}
                  logged-in? (merge {:on-click #(dispatch [:show-info-alert])})
                  (not logged-in?) (merge {:as :a
                                           :href "https://accounts.spotify.com/authorize?client_id=a88ca20494e440e3b69ae54ea7840c45&response_type=code&redirect_uri=http://localhost:3000&scope=user-modify-playback-state&code_challenge_method=S256&code_challenge=yQDgHrv-ndWb6DYGEuC44MlFfuMtU2jyYe6IodPvjmA"}))
      (if logged-in? 
        "Start Game!"
        "Connect to Spotify to Play")]]))

(defn Alert []
  (let [{:keys [title message buttons]
         :or {buttons [{:label "OK"}]} 
         :as alert} @(subscribe [:alert])]
    [:> Modal {:open (some? alert)
               :onClose #(dispatch [:close-alert])}
     [:> ModalDialog
      [:> ModalClose]
      [:> DialogTitle title]
      [:> DialogContent message]
      [:> DialogActions
       (doall
        (for [{:keys [label href on-click] :as button} (reverse buttons)]
          ^{:key label}
          [:> Button (cond-> (-> button
                                 (dissoc :label :on-click))
                       href
                       (merge {:as :a
                               :target "_blank"
                               :rel "noopener noreferrer"})
                       (and (nil? on-click) (nil? href))
                       (assoc :onClick #(dispatch [:close-alert]))
                       (coll? on-click)
                       (assoc :onClick #(dispatch on-click))
                       (fn? on-click)
                       (assoc :onClick on-click))
           label]))]]]))

(defn Main []
  (r/create-class
   {:display-name "Main"
    
    :component-did-mount 
    (fn []
      (let [params (new js/URLSearchParams (.. js/window -location -search))]
        (when (.get params "code")
          (dispatch [:login (.get params "code")]))))
    
    :reagent-render 
    (fn []
      [:<>
       [Alert]
       [:main
        [:div {:style {:width "100vw"
                       :height "100vh"
                       :display :flex
                       :align-items :center
                       :justify-content :center}}
         [:div {:style {:margin 10}}
          (if @(subscribe [:game-started?])
            [Game]
            [Start])]]]])}))