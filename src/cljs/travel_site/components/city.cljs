(ns travel-site.components.city
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.components.navbar :as navbar]
            [travel-site.router :as router]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]
            [travel-site.components.attractions :as attractions]))

;; Various util functions
(def colors ["red" "blue" "yellow" "green" "turquoise" "purple" "cyan" "yellow"])

(defn next-color [index]
  (get colors (mod index (count colors))))

(defn geojson->goog-latlng [[lng lat]]
  (js/google.maps.LatLng. lat lng))


(defn transit-directions-view [transit-directions owner]
  (reify
    om/IRender
    (render [_]
      (let [directions (-> transit-directions :directions :routes (get 0) :legs (get 0))]
        (html [:div {:class "transit-directions-view"}
               [:div {:class "ui header transit-directions-header"}
                [:div {:class "content"}
                 (:start_name transit-directions)
                 [:div {:class "sub header"} (:end_name transit-directions)]]]
               [:ul {:class "transit-steps"}
                (map #(html [:li (:instructions %)])
                     (:steps directions))]])))))

(defn transit-view [transit-journey owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment transit-view"}
             (om/build-all transit-directions-view transit-journey) ]))))

;; View to edit the current journey (i.e. change start/end locations and remove existing waypoints)
(defn attractions-selector-view [[current-city journey transit-journey] owner]
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
              :lng (-> attraction :location :coordinates (get 0))}
   :stopover true })

(defn get-transit-directions [response-channel google-driving-directions waypoints google-directions-service]
  (let [trip-legs (-> google-driving-directions :routes (get 0) :legs)]
    (reduce
      (fn [partial-directions [leg-id {:keys [start_location end_location]}]]
        (.route
          google-directions-service
          #js {:origin start_location
               :destination end_location
               :travelMode (.. js/google -maps -TravelMode -TRANSIT)}
          (fn [response status]
            (put! response-channel {:leg-id leg-id
                                    :directions response}))))
      []
      (map vector (range) trip-legs))))

;; TODO - make sure that this doesn't cause a memory leak
;; since I don't actually delete the object
(defn remove-old-renderers [owner]
  (let [prev-renderers (om/get-state owner :all-renderers)]
    (if (some? prev-renderers)
      (reduce
        (fn [_ renderer]
          (.setMap renderer nil))
        nil
        prev-renderers))))

(defn annotate-directions [start_name end_name waypoints all-directions]
  (let [all-place-names (vec (flatten (vector start_name (map #(:name %) waypoints) end_name)))]
    (sort #(compare (:leg-id %1) (:leg-id %2))
      (reduce
        (fn [partial-directions next-direction]
          (conj
            partial-directions
            {:start_name (get all-place-names (:leg-id next-direction))
             :end_name (get all-place-names (inc (:leg-id next-direction)))
             :leg-id (:leg-id next-direction)
             :directions (js->clj (:directions next-direction) :keywordize-keys true)}
            ))
        []
        all-directions))))

(defn create-renderers [owner all-directions]
  (reduce
    (fn [renderers [index directions]]
      (let [renderer (js/google.maps.DirectionsRenderer.
                       #js {:polylineOptions #js {:strokeColor (next-color index)}
                            :suppressMarkers true})]
        (.setMap renderer (om/get-state owner :google-map))
        (.setDirections renderer (clj->js (:directions directions)))
        (conj renderers renderer)))
    []
    (map vector (range) all-directions)))

(defn gen-new-renderers [owner waypoints journey all-directions]
  (let [all-renderers (create-renderers owner all-directions)]
    (om/set-state! owner :all-renderers all-renderers)
    (om/update!
      (models/transit-journey)
      (annotate-directions
        (-> journey :start-place :address)
        (-> journey :end-place :address)
        waypoints
        all-directions))))

(defn get-start-location [transit-directions]
  (-> transit-directions :directions :routes (get 0) :legs (get 0) :start_location))

(defn get-end-location [transit-directions]
  (-> transit-directions :directions :routes (get 0) :legs (get 0) :end_location))

(defn create-markers [owner]
  (reduce
    (fn [partial-markers [place-id location]]
      (conj
        partial-markers
        (js/google.maps.Marker.
          #js {:position location 
               :map (om/get-state owner :google-map)
               :title (str place-id) 
               :label (str (inc place-id)) 
               })))
    []
    (vec 
      (map vector
           "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
           (conj
             (vec (map get-start-location (models/transit-journey)))
             (get-end-location (last (models/transit-journey))))))))

;; TODO - make sure that this doesn't cause a memory leak
;; since I don't actually delete the object
(defn remove-old-markers [owner]
  (let [old-markers (om/get-state owner :all-markers)]
    (if (some? old-markers)
      (reduce
        (fn [_ marker]
          (.setMap marker nil))
        nil
        old-markers))))

(defn gen-new-markers [owner]
  (let [all-markers (create-markers owner)]
    (om/set-state! owner :all-markers all-markers)))

(defn update-journey-plan [owner]
  (let [[current-city journey] (om/get-props owner)
        waypoints (attractions/get-waypoints
                    (-> journey :waypoint-attraction-ids)
                    (-> current-city :attractions :data)) ]
    (when (and (not (empty? (-> journey :start-place :coords))) (not (empty? (-> journey :end-place :coords))))
      (.route
        (om/get-state owner :google-directions-service)
        #js {:origin (-> journey :start-place :coords clj->js)
             :destination (-> journey :end-place :coords clj->js)
             :waypoints (clj->js (map extract-goog-waypoint waypoints))
             :optimizeWaypoints true
             :travelMode (.. js/google -maps -TravelMode -DRIVING)}
        (fn [response status]
          (when (= status (.. js/google -maps -DirectionsStatus -OK))
            (let [response-channel (chan)
                  response (js->clj response :keywordize-keys true) ]
              (get-transit-directions response-channel response waypoints (om/get-state owner :google-directions-service))
              (go
                (loop [msg-count (inc (count (-> response :request :waypoints)))
                       partial-directions []]
                  (if (> msg-count 0)
                    (recur (dec msg-count) (conj partial-directions (<! response-channel)))
                    (do
                      (remove-old-renderers owner)
                      (remove-old-markers owner)
                      (gen-new-renderers owner waypoints journey partial-directions)
                      (gen-new-markers owner))))))))))))

(defn attraction-map-view [[current-city journey] owner]
  (reify
    ;; initialize the google maps objects and store them as local state to the map view
    om/IDidMount
    (did-mount [_]
      (let [google-map-container (om/get-node owner)
            google-directions-service (js/google.maps.DirectionsService.)
            google-directions-renderer (js/google.maps.DirectionsRenderer.)
            google-city-center (geojson->goog-latlng (-> current-city :city :data :center :coordinates))]
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
    (did-update [_ [next-city next-journey] _]
      (let [[city journey] (om/get-props owner)]
        (if-not (journey-same? journey next-journey)
          (update-journey-plan owner))
        (if-not (= (-> city :city :data :center :coordinates) (-> next-city :city :data :center :coordinates))
          (.setCenter (om/get-state owner :google-map) (geojson->goog-latlng (-> current-city :city :data :center :coordinates))))))

    om/IRenderState
    (render-state [this state]
      (html [:div {:class "city-map-container"} "This is where the map should go"]))))

(defn city-view [{:keys [current-city journey transit-journey]} owner]
  (reify
    ;; A bit hacky, but register a listener on the root city component whose job is to sync
    ;; changes in the journey state to the url. Not sure of a better way to do it.
    ;; Perhaps better to put it in a different component...
    om/IDidUpdate
    (did-update [_ next-props _]
      (when-not (journey-same? (:journey (om/get-props owner)) (:journey next-props))
        (router/go-to-hash
          (http/encode-url-parameters
            (str "/city/" (-> current-city :city :data :id))
            {"start-place" (js/JSON.stringify (clj->js (-> (om/get-props owner) :journey :start-place)))
             "end-place" (js/JSON.stringify (clj->js (-> (om/get-props owner) :journey :end-place)))
             "waypoint-attraction-ids[]" (clj->js (-> (om/get-props owner) :journey :waypoint-attraction-ids keys))}))))

    om/IRenderState
    (render-state [this _]
      (html [:div {:class "city-view"}
             (om/build navbar/navbar-view nil)
             [:h1 (str (-> current-city :city :data :name))]
             [:div {:class "ui centered grid"}
              [:div {:class "fourteen wide column row"}
               [:div {:class "five wide column"}
                (om/build attractions-selector-view [current-city journey transit-journey])]
               [:div {:class "nine wide column"}
                (om/build attraction-map-view [current-city journey])
                (om/build transit-view transit-journey)]]
              [:div {:class "fourteen wide column row"}
               [:div {:class "fourteen wide column"}
                (om/build attractions/all-attractions-view [(-> current-city :attraction_categories :data)
                                                            (-> current-city :attractions :data)])]]
              ]]))))
