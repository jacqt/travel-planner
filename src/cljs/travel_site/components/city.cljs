(ns travel-site.components.city
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]))

(defn place-view [place owner]
  (reify
    om/IRender
    (render [_]
      (html [:div (:place-name place)]))))

(defn place-category-view [category owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment place-category-view"}
             [:h1 (:category-name category)]
             (om/build-all place-view (:places category))]))))

(defn places-selector-view [current-city owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment places-selector-view"}
             [:h1 "Plan your trip!"]
             [:div {:class "ui form twelve wide column centered"}
              [:div {:class "field"}
               [:div {:class "ui fluid input"}
               (om/build inputs/address-autocomplete-input [(-> current-city :journey)
                                                            {:edit-key :start-address
                                                             :className "start-address-input" ;;TODO - rename className -> class
                                                             :placeholder-text "Start address"}])]]
              [:div {:class "field"}
               [:div {:class "ui fluid input"}
               (om/build inputs/address-autocomplete-input [(-> current-city :journey)
                                                            {:edit-key :end-address
                                                             :className "end-address-input" ;; TODO - rename className -> class
                                                             :placeholder-text "End address"}])]]
              ]
             ])
      )
    )
  )

(defn place-map-view [state owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html [:div {:class "city-map-container"} "This is where the map should go"])
      )
    )
  )

(defn city-view [{:keys [current-city]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "dashboard-panel"}
             [:pre (with-out-str (pprint current-city))]
             [:h1 (str "City id: " (:id current-city))]
             [:div {:class "ui centered grid"}
              [:div {:class "fourteen wide column row"}
               [:div {:class "five wide column"}
                (om/build places-selector-view current-city)
                ]
               [:div {:class "nine wide column"}
                (om/build place-map-view nil)
                ]
               ]
              ]]))))
