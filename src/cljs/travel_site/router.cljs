(ns travel-site.router
  (:require [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [travel-site.models :as models]
            [om.core :as om :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))


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
      (when (some? (:waypoint-place-ids query-params))
        (om/update!
          (-> app-state :current-city :journey :waypoint-place-ids)
          (vec (map int (:waypoint-place-ids query-params)))))
      (om/update! app-state :route "city"))))

; enable fallback that don't have HTML 5 History
(secretary/set-config! :prefix "#")

; Quick and dirty history configuration.
(let [h (History. false nil (js/document.getElementById "goog-hidden-input"))]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
