(ns travel-site.router
  (:require [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [travel-site.models :as models]
            [om.core :as om :include-macros true]
            [travel-site.utils.http :as http]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))


(defn process-query-params [app-state query-params]
  (when (some? (:waypoint-attraction-ids query-params))
    (om/update!
      (-> app-state :journey :waypoint-attraction-ids)
      (vec (map int (:waypoint-attraction-ids query-params)))))
  (om/update! app-state :route "city"))

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
      (if-not (= (int id) (-> app-state :current-city :id))
        (http/get-city
          id
          (fn [city]
            (om/update! app-state :current-city city)
            (process-query-params app-state query-params)))
        (process-query-params app-state query-params))
      )))

; enable fallback that don't have HTML 5 History
(secretary/set-config! :prefix "#")

; Quick and dirty history configuration.
(let [h (History. false nil (js/document.getElementById "goog-hidden-input"))]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
