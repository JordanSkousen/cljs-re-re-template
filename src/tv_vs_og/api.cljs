(ns tv-vs-og.api
  (:require [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx]]))

(defn- auth-header 
  [db]
  {:Authorization (str "Bearer " (get-in db [:local-storage :access-token]))})

(defn- fail-retry
  [dispatch-coll]
  (fn [_ response]
    (js/console.warn "fail-retry callback, response:" response)
    (if (= 401 (:status response))
      (dispatch [::refresh-access-token dispatch-coll])
      (dispatch [::fail response]))))

(reg-event-fx
 ::exchange-login-code
 (fn [_ [_ code]]
   {:http-post ["https://accounts.spotify.com/api/token"
                {:params {"client_id" "a88ca20494e440e3b69ae54ea7840c45"
                          "code" code
                          "grant_type" "authorization_code"
                          "redirect_uri" "http://localhost:3000"
                          "code_verifier" "5LlsVjhljoR2XGbJs99Z55201QJJREJCqpBmuU8cmdNo6q9X7bNeNxZFCrvpAcs08Hg9w6VZV89tsMP9tpMygT6E5qE0NrDvnEQDRTofGJD27KQ4s5M6G4Koip8CTNcv"}}
                :exchange-login-code-success
                (fail-retry [::exchange-login-code code])]}))

(reg-event-fx
 ::refresh-access-token
 (fn [{:keys [db]} [_ dispatch-coll]]
   {:http-post ["https://accounts.spotify.com/api/token"
                {:params {"client_id" "a88ca20494e440e3b69ae54ea7840c45"
                          "grant_type" "refresh_token"
                          "refresh_token" (get-in db [:local-storage :refresh-token])}}
                [:refresh-access-token-success dispatch-coll]
                ::fail]}))

(reg-event-fx
 ::get-user-profile
 (fn [{:keys [db]}]
   {:http-get ["https://api.spotify.com/v1/me"
               {:headers (auth-header db)}
               :get-user-profile-success
               (fail-retry [::get-user-profile])]}))

(reg-event-fx
 ::get-track
 (fn [{:keys [db]} [_ track-id]]
   {:http-get [(str "https://api.spotify.com/v1/tracks/" track-id)
               {:headers (auth-header db)}
               :get-track-success
               (fail-retry [::get-track track-id])]}))

(reg-event-fx
 ::player-play
 (fn [{:keys [db]} [_ {:keys [track position] :as config}]]
   {:http-put ["https://api.spotify.com/v1/me/player/play"
               {:headers (auth-header db)
                :body {"uris" [(str "spotify:track:" track)]
                       "position_ms" (* position 1000)}}
               :player-play-success
               (fail-retry [::player-play config])]}))

(reg-event-fx
 ::fail
 (fn [_ [_ response]]
   (js/console.error "API request failed:" response)
   {:dispatch [:error "Login failed" "Oops, something went wrong with the last API request. Please try again."]}))