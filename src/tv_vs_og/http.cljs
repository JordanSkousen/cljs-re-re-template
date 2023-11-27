(ns tv-vs-og.http
  (:require [re-frame.core :refer [dispatch reg-event-fx reg-fx]]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(defn prep-event [event]
  (if (keyword? event)
    [event]
    event))

(defn http-request
  [{:keys [url method headers body params then catch]}]
  {:http-xhrio
   (cond-> {:method           method
            :uri              url
            :headers          (merge {"Content-Type" "application/json"}
                                     headers)
            :params           params
            :format           (if body (ajax/json-request-format) (ajax/url-request-format))
            :response-format  (ajax/json-response-format {:keywords? true})
            :on-success       (if (fn? then)
                                [::http-response-fn then]
                                (prep-event then))
            :on-failure       (if (fn? catch)
                                [::http-response-fn catch]
                                (prep-event catch))}
     body (assoc :body (-> body clj->js js/JSON.stringify)))})

(defn http-fx-body
  [method [url config then catch]]
  (let [config' (-> (if (or (contains? config :headers)
                            (contains? config :body)
                            (contains? config :params))
                      config
                      {:body config})
                    (merge {:url url
                            :method method
                            :then then
                            :catch catch}))]
    (dispatch [::http-request config'])))

(reg-fx
 :http-get
 (partial http-fx-body :get))
(reg-fx
 :http-post
 (partial http-fx-body :post))
(reg-fx
 :http-put
 (partial http-fx-body :put))
(reg-fx
 :http-delete
 (partial http-fx-body :delete))
(reg-fx
 :http-patch
 (partial http-fx-body :patch))

(reg-event-fx
 ::http-request
 (fn [_ [_ config]]
   (http-request config)))

(reg-event-fx
 ::http-response-fn
 (fn [{:keys [db]} [_ f response]]
   (f db response)))