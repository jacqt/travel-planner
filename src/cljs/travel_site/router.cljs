(ns travel-site.router
  (:require [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [travel-site.models :as models]
            [om.core :as om :include-macros true]
            [travel-site.utils.http :as http]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))


;; TODO remove code duplication here
(defn process-query-params [app-state query-params]
  (when (some? (:wp-ids query-params))
    (om/update!
      (-> app-state :journey :waypoint-attraction-ids)
      (reduce
        #(assoc %1 %2 true) {}
        (map int (:wp-ids query-params)))))
  (when (some? (:start-addr query-params))
    (om/update!
      (-> app-state :journey :start-place)
      {:address (:start-addr query-params)
       :coords {:lat (js/parseFloat (:start-lat query-params))
                :lng (js/parseFloat (:start-lng query-params))}}))
  (when (some? (:end-addr query-params))
    (om/update!
      (-> app-state :journey :end-place)
      {:address (:end-addr query-params)
       :coords {:lat (js/parseFloat (:end-lat query-params))
                :lng (js/parseFloat (:end-lng query-params))}})))

(defn go-to-hash [new-hash]
  (aset js/window.location "hash" new-hash))

(defn route-app []
  (defroute
    "/" []
    (let [app-state (models/whole-state)]
      (om/update! app-state :route "home")))

  (defroute
    "/city/:id" [id query-params]
    (let [app-state (models/whole-state)]
      (om/update! app-state :route "city")
      (if-not (= (int id) (-> app-state :current-city :id))
        (http/get-city
          id
          (fn [city]
            (om/update! app-state :current-city city)
            (process-query-params app-state query-params)))
        (process-query-params app-state query-params)))))

; enable fallback that don't have HTML 5 History
(secretary/set-config! :prefix "#")

; Quick and dirty history configuration.
(let [h (History. false nil (js/document.getElementById "goog-hidden-input"))]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
