(ns travel-site.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [travel-site.utils.auth :as auth]))

(enable-console-print!)
(defonce app-state (atom {:credentials (auth/get-credentials)
                          :route "home"
                          :current-city {:name "London"
                                         :city-center {:lat 0
                                                       :lng 0}
                                         :id -1
                                         :attraction-categories []
                                         :attractions []}
                          :journey {:start-place {:address ""
                                                       :coords {}}
                                    :end-place {:address ""
                                                     :coords {}}
                                    :waypoint-attraction-ids {}}
                          :transit-journey {}
                          :cities [{:city-name "London"
                                    :id 1}
                                   {:city-name "Paris"
                                    :id 2}]}))

(defn whole-state []
  (om/ref-cursor (om/root-cursor app-state)))

(defn cities []
  (om/ref-cursor (:cities (om/root-cursor app-state))))

(defn current-city []
  (om/ref-cursor (:current-city (om/root-cursor app-state))))

(defn waypoint-attraction-ids []
  (om/ref-cursor (-> (om/root-cursor app-state) :journey :waypoint-attraction-ids)))

(defn transit-journey []
  (om/ref-cursor (:transit-journey (om/root-cursor app-state))))
