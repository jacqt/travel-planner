(ns travel-site.components.city
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]
            [travel-site.components.places :as places]))


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
               (om/build places/waypoints-selector-view current-city)]]]))))

(defn journey-same? [previous-journey next-journey]
  (and
    (= (-> previous-journey :waypoint-place-ids) (-> next-journey :waypoint-place-ids))
    (= (-> previous-journey :start-place :coords) (-> next-journey :start-place :coords))
    (= (-> previous-journey :end-place :coords) (-> next-journey :end-place :coords))))

(defn update-journey-plan [owner]
  (let [current-city (om/get-props owner)]
    (js/console.log (and (not (empty? (-> current-city :journey :start-place :coords))) (not (empty? (-> current-city :journey :end-place :coords)))))
    (when (and
            (not (empty? (-> current-city :journey :start-place :coords)))
            (not (empty? (-> current-city :journey :end-place :coords))))
      (.route
        (om/get-state owner :google-directions-service)
        #js {:origin (-> current-city :journey :start-place :coords clj->js)
             :destination (-> current-city :journey :end-place :coords clj->js)
             :waypoints (clj->js 
                          (places/get-waypoints (-> current-city :journey :waypoint-place-ids) (:places current-city)))
             :optimizeWaypoints true
             :travelMode (.. js/google -maps -TravelMode -DRIVING)}
        (fn [response, status]
          (js/console.log response)
          (when (= status (.. js/google -maps -DirectionsStatus -OK))
            (.setDirections (om/get-state owner :google-directions-renderer) response)))))))

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

    om/IDidUpdate
    (did-update [_ next-props _]
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
                (om/build place-map-view current-city)]]
              [:div {:class "fourteen wide column row"}
               [:div {:class "fourteen wide column"}
                (om/build places/all-places-view [(:place-categories current-city) (:places current-city)])]]]]))))
