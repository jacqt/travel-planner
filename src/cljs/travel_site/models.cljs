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
                                         :attraction-categories [{:category-name "Parks"
                                                                  :id 1}
                                                                 {:category-name "Restaurants"
                                                                  :id 2}]
                                         :attractions [{:name "Hyde Park"
                                                        :category-id 1
                                                        :description "A beautiful park in London"
                                                        :location "Hyde Park, London"
                                                        :id 1}
                                                       {:attraction-name "Imperial College"
                                                        :category-id 1
                                                        :description "A great college to go to for a relaxing time!"
                                                        :location "Imperial College, London"
                                                        :id 3}
                                                       {:attraction-name "Duck & Waffle"
                                                        :category-id 2
                                                        :description "The tallest restaurant in London!"
                                                        :location "The Heron Tower, 110 Bishopsgate, London"
                                                        :id 2} ]
                                         :computed-tour []}
                          :journey {:start-attraction {:address ""
                                                       :coords {}}
                                    :end-attraction {:address ""
                                                     :coords {}}
                                    :waypoint-attraction-ids [] }
                          :cities [{:city-name "London"
                                    :id 1}
                                   {:city-name "Oxford"
                                    :id 2}
                                   {:city-name "Paris"
                                    :id 3} ]}))
;(defonce app-state (atom {:credentials (auth/get-credentials)
;:route "home"
;:current-city {:city-name "London"
;:city-center {:lat 51.51786
;:lng -0.102216}
;:id 1
;:attraction-categories [{:category-name "Parks"
;:id 1}
;{:category-name "Restaurants"
;:id 2}]
;:attractions [{:attraction-name "Hyde Park"
;:category-id 1
;:description "A beautiful park in London"
;:location "Hyde Park, London"
;:id 1}
;{:attraction-name "Imperial College"
;:category-id 1
;:description "A great college to go to for a relaxing time!"
;:location "Imperial College, London"
;:id 3}
;{:attraction-name "Duck & Waffle"
;:category-id 2
;:description "The tallest restaurant in London!"
;:location "The Heron Tower, 110 Bishopsgate, London"
;:id 2} ]
;:journey {:start-attraction {:address ""
;:coords {}}
;:end-attraction {:address ""
;:coords {}}
;:waypoint-attraction-ids []
;}
;:computed-tour []}
;:cities [{:city-name "London"
;:id 1}
;{:city-name "Oxford"
;:id 2}
;{:city-name "Paris"
;:id 3} ]}))

(defn whole-state []
  (om/ref-cursor (om/root-cursor app-state)))

(defn cities []
  (om/ref-cursor (:cities (om/root-cursor app-state))))

(defn current-city []
  (om/ref-cursor (:current-city (om/root-cursor app-state))))

(defn waypoint-attraction-ids []
  (om/ref-cursor (-> (om/root-cursor app-state) :journey :waypoint-attraction-ids)))
