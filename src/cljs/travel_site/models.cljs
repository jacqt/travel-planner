(ns travel-site.models
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [travel-site.utils.auth :as auth]))

(enable-console-print!)
(defonce app-state (atom {:credentials (auth/get-credentials)
                          :route nil
                          :current-city {:city-name "London"
                                         :id 1
                                         :place-categories [{:category-name "Parks"
                                                             :id 1
                                                             :places [{:place-name "Hyde Park"
                                                                       :addresses ["address 1"]
                                                                       :id 1}]}
                                                            {:category-name "Restaurants"
                                                             :id 2
                                                             :places [{:place-name "Nandos"
                                                                       :addresses ["address 1" "address 2" "address 3"]
                                                                       :id 2}] }
                                                            ]
                                         :selected-places []
                                         :journey {}
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
