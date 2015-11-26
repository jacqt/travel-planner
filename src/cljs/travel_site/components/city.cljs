(ns travel-site.components.city
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]
            [travel-site.components.attractions :as attractions]))


(defn attractions-selector-view [[current-city journey] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment attractions-selector-view"}
             [:h1 "Plan your trip!"]
             [:div {:class "ui form twelve wide column centered"}
              [:div {:class "field"}
               [:label "Start address"]
               [:div {:class "ui fluid input"}
                (om/build inputs/address-autocomplete-input [(-> journey :start-attraction)
                                                             {:edit-key :address
                                                              :coords-key :coords
                                                              :className "start-address-input" ;;TODO - rename className -> class
                                                              :attractionholder-text "Start address"}])]]
              [:div {:class "field"}
               [:label "End address"]
               [:div {:class "ui fluid input"}
                (om/build inputs/address-autocomplete-input [(-> journey :end-attraction)
                                                             {:edit-key :address
                                                              :coords-key :coords
                                                              :className "end-address-input" ;; TODO - rename className -> class
                                                              :attractionholder-text "End address"}])]]
              [:div {:class "field"}
               (om/build attractions/waypoints-selector-view [current-city journey])]]]))))

(defn journey-same? [previous-journey next-journey]
  (and
    (= (-> previous-journey :waypoint-attraction-ids) (-> next-journey :waypoint-attraction-ids))
    (= (-> previous-journey :start-attraction :coords) (-> next-journey :start-attraction :coords))
    (= (-> previous-journey :end-attraction :coords) (-> next-journey :end-attraction :coords))))

(defn extract-goog-waypoint [attraction]
  {:location {:lat (-> attraction :location :coordinates (get 1))
              :lng (-> attraction :location :coordinates (get 0)) }
   :stopover true })

(defn update-journey-plan [owner]
  (let [[current-city journey] (om/get-props owner)]
    (when (and
            (not (empty? (-> journey :start-attraction :coords)))
            (not (empty? (-> journey :end-attraction :coords))))
      (.route
        (om/get-state owner :google-directions-service)
        #js {:origin (-> journey :start-attraction :coords clj->js)
             :destination (-> journey :end-attraction :coords clj->js)
             :waypoints (clj->js
                          (map
                            extract-goog-waypoint
                            (attractions/get-waypoints
                              (-> journey :waypoint-attraction-ids)
                              (-> current-city :attractions :data))))
             :optimizeWaypoints true
             :travelMode (.. js/google -maps -TravelMode -DRIVING)}
        (fn [response, status]
          (js/console.log response)
          (when (= status (.. js/google -maps -DirectionsStatus -OK))
            (.setDirections (om/get-state owner :google-directions-renderer) response)))))))

(defn attraction-map-view [[current-city journey] owner]
  (reify
    ;; initialize the google maps objects and store them as local state to the map view
    om/IDidMount
    (did-mount [_]
      (let [google-map-container (om/get-node owner)
            google-directions-service (js/google.maps.DirectionsService.)
            google-directions-renderer (js/google.maps.DirectionsRenderer.)
            google-city-center (js/google.maps.LatLng.
                                 (-> current-city :city :data :center :coordinates (get 1)) ;; TODO - flip to 0, after david fixes data source...
                                 (-> current-city :city :data :center :coordinates (get 0)))]
        (let [google-map (js/google.maps.Map.
                           google-map-container
                           #js {:center google-city-center
                                :zoom 9})]
          (.setMap google-directions-renderer google-map)
          (om/set-state! owner :google-map google-map)
          (om/set-state! owner :google-directions-service google-directions-service)
          (om/set-state! owner :google-directions-renderer google-directions-renderer)
          (update-journey-plan owner))))

    ;; recompute the route upon journey update
    om/IDidUpdate
    (did-update [_ [_ next-journey] _]
      (when-not (journey-same? (get (om.core/get-props owner) 1) next-journey)
        (js/console.log "Updating the rendered journey")
        (update-journey-plan owner)))

    om/IRenderState
    (render-state [this state]
      (html [:div {:class "city-map-container"} "This is where the map should go"]))))

(defn city-view [{:keys [current-city journey]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "city-view"}
             [:h1 (str (-> current-city :city :data :name))]
             [:div {:class "ui centered grid"}
              [:div {:class "fourteen wide column row"}
               [:div {:class "five wide column"}
                (om/build attractions-selector-view [current-city journey])]
               [:div {:class "nine wide column"}
                (om/build attraction-map-view [current-city journey])]]
              [:div {:class "fourteen wide column row"}
               [:div {:class "fourteen wide column"}
                (om/build attractions/all-attractions-view [(-> current-city :attraction_categories :data)
                                                  (-> current-city :attractions :data)])]]
              [:pre (print-str current-city)]
              [:pre (print-str journey)]
              ]]))))

