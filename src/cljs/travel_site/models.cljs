(ns travel-site.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [travel-site.utils.auth :as auth]))

(enable-console-print!)
(defonce app-state (atom {:credentials (auth/get-credentials)
                          :route nil
                          :current-city {:city-name "London"
                                         :city-center {:lat 51.51786
                                                       :lng -0.102216}
                                         :id 1
                                         :place-categories [{:category-name "Parks"
                                                             :id 1
                                                             :places [{:place-name "Hyde Park"
                                                                       :location "Hyde Park, London"
                                                                       :id 1}]}
                                                            {:category-name "Restaurants"
                                                             :id 2
                                                             :places [{:place-hame "Duck & Waffle"
                                                                       :location "The Heron Tower, 110 Bishopsgate, London"
                                                                       :id 1}]}
                                                            ]
                                         :selected-places []
                                         :journey {:start-place {:address ""
                                                                 :coords {}}
                                                   :end-place {:address ""
                                                               :coords {}}
                                                   :waypoints [{:location "Oxford, UK"
                                                                :stopover true }]
                                                   }
                                         :computed-tour []}
                          :cities [{:city-name "London"
                                    :id 1}
                                   {:city-name "Oxford"
                                    :id 2}
                                   {:city-name "Paris"
                                    :id 3} ]}))

(defn cities []
  (om/ref-cursor (:cities (om/root-cursor app-state))))

(defn current-city []
  (om/ref-cursor (:current-city (om/root-cursor app-state))))

(defn current-city-journey-waypoints []
  (om/ref-cursor (-> (om/root-cursor app-state) :current-city :journey :waypoints)))
