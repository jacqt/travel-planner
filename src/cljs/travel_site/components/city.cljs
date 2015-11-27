(ns travel-site.components.city
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.router :as router]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]
            [travel-site.components.attractions :as attractions]))


;; View to edit the current journey (i.e. change start/end locations and remove existing waypoints)
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
                (om/build inputs/address-autocomplete-input [(-> journey :start-place)
                                                             {:edit-key :address
                                                              :coords-key :coords
                                                              :className "start-address-input" ;;TODO - rename className -> class
                                                              :attractionholder-text "Start address"}])]]
              [:div {:class "field"}
               [:label "End address"]
               [:div {:class "ui fluid input"}
                (om/build inputs/address-autocomplete-input [(-> journey :end-place)
                                                             {:edit-key :address
                                                              :coords-key :coords
                                                              :className "end-address-input" ;; TODO - rename className -> class
                                                              :attractionholder-text "End address"}])]]
              [:div {:class "field"}
               (om/build attractions/waypoints-selector-view [current-city journey])]]]))))


;; Functions for the map view.
(defn journey-same? [previous-journey next-journey]
  (and
    (= (-> previous-journey :waypoint-attraction-ids) (-> next-journey :waypoint-attraction-ids))
    (= (-> previous-journey :start-place :coords) (-> next-journey :start-place :coords))
    (= (-> previous-journey :end-place :coords) (-> next-journey :end-place :coords))))

(defn extract-goog-waypoint [attraction]
  {:location {:lat (-> attraction :location :coordinates (get 1))
              :lng (-> attraction :location :coordinates (get 0)) }
   :stopover true })

(defn get-transit-directions [response-channel google-driving-directions google-directions-service]
  (let [trip-legs (-> google-driving-directions :routes (get 0) :legs)]
    (reduce
      (fn [partial-directions {:keys [start_location end_location]}]
        (.route
          google-directions-service
          #js {:origin start_location
               :destination end_location
               :travelMode (.. js/google -maps -TravelMode -TRANSIT)}
          (fn [response status]
            (put! response-channel response)
            )))
      []
      trip-legs)))

(def colors ["red" "blue" "yellow" "green" "turquoise" "purple" "cyan" "yellow"])

(defn next-color [index]
  (get colors (mod index (count colors))))

(defn update-journey-plan [owner]
  (let [[current-city journey] (om/get-props owner)]
    (when (and
            (not (empty? (-> journey :start-place :coords)))
            (not (empty? (-> journey :end-place :coords))))
      (.route
        (om/get-state owner :google-directions-service)
        #js {:origin (-> journey :start-place :coords clj->js)
             :destination (-> journey :end-place :coords clj->js)
             :waypoints (clj->js
                          (map
                            extract-goog-waypoint
                            (attractions/get-waypoints
                              (-> journey :waypoint-attraction-ids)
                              (-> current-city :attractions :data))))
             :optimizeWaypoints true
             :travelMode (.. js/google -maps -TravelMode -DRIVING)}
        (fn [response status]
          (when (= status (.. js/google -maps -DirectionsStatus -OK))
            ;; TODO make this give back transit directions rather than driving directions
            ;; Potenial problems:
            ;;  * Need to specify start/end times
            ;;  * Timezones will be an issue *sigh*
            (js/console.log "Here we go!!" response)
            (let [response-channel (chan)
                  response (js->clj response :keywordize-keys true) ]
              (get-transit-directions response-channel response (om/get-state owner :google-directions-service))
              (go
                (loop [msg-count (inc (count (-> response :request :waypoints)))
                       partial-directions []]
                  (if (> msg-count 0)
                    (recur (dec msg-count) (conj partial-directions (<! response-channel)))
                    (do
                      (let [prev-renderers (om/get-state owner :all-renderers)]
                        (if (some? prev-renderers)
                          (reduce
                            (fn [_ renderer]
                              (.setMap renderer nil))
                            nil
                            prev-renderers)))

                      (let [all-renderers (reduce
                                            (fn [renderers [index directions]]
                                              (js/console.log "Rendering one leg of the thing")
                                              (let [renderer (js/google.maps.DirectionsRenderer.
                                                               #js {:polylineOptions #js {:strokeColor (next-color index)}})]
                                                (.setMap renderer (om/get-state owner :google-map))
                                                (.setDirections renderer (clj->js directions))
                                                (conj renderers renderer)))
                                            []
                                            (map vector (range) partial-directions))]
                        (om/set-state! owner :all-renderers all-renderers)
                        ))))
                ))))))))

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
    ;; A bit hacky, but register a listener on the root city component whose job is to sync
    ;; changes in the journey state to the url. Not sure of a better way to do it.
    ;; Perhaps better to put it in a different component...
    om/IDidUpdate
    (did-update [_ next-props _]
      (when-not (journey-same? (:journey (om.core/get-props owner)) (:journey next-props))
        (router/go-to-hash
          (http/encode-url-parameters
            (str "/city/" (-> current-city :city :data :id))
            {"start-place" (js/JSON.stringify (clj->js (-> (om.core/get-props owner) :journey :start-place)))
             "end-place" (js/JSON.stringify (clj->js (-> (om.core/get-props owner) :journey :end-place)))
             "waypoint-attraction-ids[]" (clj->js (-> (om.core/get-props owner) :journey :waypoint-attraction-ids))}))))

    om/IRenderState
    (render-state [this _]
      (html [:div {:class "city-view"}
             [:pre (print-str current-city)]
             [:pre (print-str journey)]
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
              ]]))))

