(ns travel-site.components.city
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]))

(defn place-view [place owner]
  (reify
    om/IRender
    (render [_]
      (html [:div
             (:place-name place)
             [:button {:class "ui button"}
              "Add"]
             ]))))

(defn place-category-view [category owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment place-category-view"}
             [:h3 (:category-name category)]
             (om/build-all place-view (:places category))]))))

(defn waypoints-selector-view [place-categories owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html [:div
             [:h2 "Waypoints"]
             (om/build-all place-category-view place-categories)]))))

(defn places-selector-view [current-city owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment places-selector-view"}
             [:h1 "Plan your trip!"]
             [:div {:class "ui form twelve wide column centered"}
              [:div {:class "field"}
               [:div {:class "ui fluid input"}
               (om/build inputs/address-autocomplete-input [(-> current-city :journey :start-place)
                                                            {:edit-key :address
                                                             :coords-key :coords
                                                             :className "start-address-input" ;;TODO - rename className -> class
                                                             :placeholder-text "Start address"}])]]
              [:div {:class "field"}
               [:div {:class "ui fluid input"}
               (om/build inputs/address-autocomplete-input [(-> current-city :journey :end-place)
                                                            {:edit-key :address
                                                             :coords-key :coords
                                                             :className "end-address-input" ;; TODO - rename className -> class
                                                             :placeholder-text "End address"}])]]
              [:div {:class "field"}
               (om/build waypoints-selector-view (:place-categories current-city))]]])
      )
    )
  )

(defn journey-same? [previous-journey next-journey]
  (and
    (= (-> previous-journey :start-place :coords) (-> next-journey :start-place :coords))
    (= (-> previous-journey :end-place :coords) (-> next-journey :end-place :coords))))

(defn update-journey-plan [owner]
  (let [current-city (om/get-props owner)]
    (.route
      (om/get-state owner :google-directions-service)
      #js {:origin (-> current-city :journey :start-place :address)
           :destination (-> current-city :journey :end-place :address)
           :waypoints (-> current-city :journey :waypoints clj->js)
           :optimizeWaypoints true
           :travelMode (.. js/google -maps -TravelMode -DRIVING)}
      (fn [response, status]
        (js/console.log response)
        (when (= status (.. js/google -maps -DirectionsStatus -OK))
          (.setDirections (om/get-state owner :google-directions-renderer) response))))))

(defn place-map-view [current-city owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [google-map-container (om/get-node owner)
            google-directions-service (js/google.maps.DirectionsService.)
            google-directions-renderer (js/google.maps.DirectionsRenderer.)
            google-city-center (js/google.maps.LatLng.
                                 (-> current-city :city-center :lat)
                                 (-> current-city :city-center :lng))]
        (let [google-map (js/google.maps.Map.
                           google-map-container
                           #js {:center google-city-center
                                :zoom 9})]
          (.setMap google-directions-renderer google-map)
          (om/set-state! owner :google-map google-map)
          (om/set-state! owner :google-directions-service google-directions-service)
          (om/set-state! owner :google-directions-renderer google-directions-renderer)
          (update-journey-plan owner))))

    om/IWillUpdate
    (will-update [_ next-props _]
      (when-not (journey-same? (:journey (om.core/get-props owner)) (:journey next-props))
        (js/console.log "Updating the rendered journey")
        (update-journey-plan owner)))

    om/IRenderState
    (render-state [this state]
      (html [:div {:class "city-map-container"} "This is where the map should go"]))))

(defn city-view [{:keys [current-city]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "city-view"}
             [:pre (with-out-str (pprint (-> current-city :journey)))]
             [:h1 (str "City id: " (:id current-city))]
             [:div {:class "ui centered grid"}
              [:div {:class "fourteen wide column row"}
               [:div {:class "five wide column"}
                (om/build places-selector-view current-city)]
               [:div {:class "nine wide column"}
                (om/build place-map-view current-city)]]]]))))
