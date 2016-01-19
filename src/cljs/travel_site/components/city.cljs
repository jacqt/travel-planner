(ns travel-site.components.city
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [goog.string :as gstring]
            [goog.string.format]
            [travel-site.components.navbar :as navbar]
            [travel-site.router :as router]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.utils.util-funcs :as util-funcs]
            [travel-site.utils.constants :as constants]
            [travel-site.models :as models]
            [travel-site.components.attractions :as attractions]))

;; Various util functions
(def colors (flatten (repeat ["green" "blue" "#66CD00" "#03A89E" "#83F52C" "#4C7064"])))
(def widths (flatten (repeat [6.0 4.0])))
(def london-feb-5-2015-9am (js/Date. 2015 1 5 9 0 0 0))

(defn is-loading? [temporary-state]
  (:is-loading temporary-state))

(defn animate-scroll-to-offset [scroll-offset]
  (-> "html, body" js/$. (.animate #js {:scrollTop scroll-offset})))

(defn animate-scroll-to-element [element]
  (animate-scroll-to-offset (-> element js/$. .offset (aget "top"))))

(defn strict-map [map-func map-over]
  (reduce
    (fn [_ v]
      (map-func v))
    []
    map-over))

(defn geojson->goog-latlng [[lng lat]]
  (js/google.maps.LatLng. lat lng))

(defn get-start-location [transit-directions]
  (-> transit-directions :directions :routes (get 0) :legs (get 0) :start_location))

(defn get-end-location [transit-directions]
  (-> transit-directions :directions :routes (get 0) :legs (get 0) :end_location))

(defn extend-duration-with-step [orig-length step]
  (-> step :duration :value (+ orig-length)))

(defn length-of-directions [direction]
  (let [num-seconds (reduce extend-duration-with-step 0 (:steps direction))]
    (str (gstring/format "%.0f" (/ num-seconds 60)) " minutes")))

(defn transit-step-view [transit-step owner]
  (reify
    om/IRender
    (render [_]
      (html [:li
             [:b (:instructions transit-step)]
             [:i (str
                   " ("
                   (-> transit-step :distance :text)
                   " - "
                   (-> transit-step :duration :text)
                   (if (= "TRANSIT" (:travel_mode transit-step))
                     (str  " - " (-> transit-step :transit :num_stops) " stops"))
                   ")")]
             (when (= "TRANSIT" (:travel_mode transit-step))
               [:div
                [:div
                 "Take the " (-> transit-step :transit :line :short_name)
                 " from "
                 [:b (-> transit-step :transit :departure_stop :name)] " to "
                 [:b (-> transit-step :transit :arrival_stop :name)]]])]))))

(defn transit-directions-view [transit-directions owner]
  (reify
    om/IRender
    (render [_]
      (let [directions (-> transit-directions :directions :routes (get 0) :legs (get 0))]
        (html [:div {:class "transit-directions-view"}
               [:div {:class "ui header transit-directions-header"}
                [:div {:class "content"}
                 (:start_name transit-directions)
                 [:div {:class "sub header"}
                  (:end_name transit-directions) " - " (length-of-directions directions)]]]
               [:ul {:class "transit-steps"}
                (om/build-all transit-step-view (:steps directions))]])))))

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
      (html [:div {:class "ui basic segment fourteen wide attractions-selector-view"}
             [:div {:class "ui form twelve wide grid column"}

              ;; Computer only - put the inputs side by side
              [:div {:class "inline fields computer tablet only row"}
               [:div {:class "sixteen wide field"}
                [:div {:class "ui fluid input address-input"}
                 (om/build inputs/address-autocomplete-input [(-> journey :start-place)
                                                              {:edit-key :address
                                                               :coords-key :coords
                                                               :className "address-fields" ;;TODO - rename className -> class
                                                               :placeholder-text "Start address"}])]
                [:div {:class "ui fluid input address-input"}
                 (om/build inputs/address-autocomplete-input [(-> journey :end-place)
                                                              {:edit-key :address
                                                               :coords-key :coords
                                                               :className "address-fields" ;; TODO - rename className -> class
                                                               :placeholder-text "End address"}])]
                [:div {:class "ui red button go-button"
                       :on-click #(animate-scroll-to-offset 550)}
                 "Go"]]]

              ;; mobile and tablet - put the inputs on top of each other
              [:div {:class "inline fields ui mobile only row"}
               [:div {:class "sixteen wide field"}
                [:div {:class "ui fluid input address-input"}
                 (om/build inputs/address-autocomplete-input [(-> journey :start-place)
                                                              {:edit-key :address
                                                               :coords-key :coords
                                                               :className "address-fields" ;;TODO - rename className -> class
                                                               :placeholder-text "Start address"}])]]
                [:div {:class "sixteen wide field"}
                 [:div {:class "ui fluid input address-input"}
                  (om/build inputs/address-autocomplete-input [(-> journey :end-place)
                                                               {:edit-key :address
                                                                :coords-key :coords
                                                                :className "address-fields" ;; TODO - rename className -> class
                                                                :placeholder-text "End address"}])]]]]]))))


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
               :transitOptions #js {:departureTime london-feb-5-2015-9am}
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

(defn get-distance [waypoint origin]
  (let [waypoint-coord (-> waypoint :location :coordinates)]
    (apply + (map #(* (- %1 %2) (- %1 %2)) waypoint-coord [(:lng origin) (:lat origin)]))))

(defn find-closest-waypoint [waypoints direction]
  (let [origin (js->clj (.toJSON (aget (clj->js direction) "directions" "request" "origin"))
                        :keywordize-keys true)]
    (:waypoint
      (reduce
        (fn [closest-waypoint next-waypoint]
          (let [distance (get-distance next-waypoint origin)]
            (if (< distance (:distance closest-waypoint))
              {:waypoint next-waypoint
               :distance distance}
              closest-waypoint)))
        {:waypoint (first waypoints)
         :distance (get-distance (first waypoints) origin)}
        (rest waypoints)))))

(defn rearrange-waypoint-order [waypoints directions]
  (reduce
    (fn [new-waypoints direction]
      (conj new-waypoints (find-closest-waypoint waypoints direction)))
    []
    (rest (sort #(compare (:leg-id %1) (:leg-id %2)) directions))))

(defn annotate-directions [start_name end_name waypoints all-directions]
  (let [waypoints (rearrange-waypoint-order waypoints all-directions)]
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
              all-directions)))))

(defn make-transit-journey [waypoints journey all-directions]
  (annotate-directions
    (-> journey :start-place :address)
    (-> journey :end-place :address)
    waypoints
    all-directions))

(defn construct-renderer [index directions google-map]
  (let [renderer (js/google.maps.DirectionsRenderer.
                   #js {:polylineOptions #js {:strokeColor (nth colors index)
                                              :strokeOpacity 0.6
                                              :geodesic true
                                              :strokeWeight (nth widths index)}
                        :preserveViewport true
                        :suppressInfoWindows true
                        :suppressMarkers true}) ]
    (.setMap renderer google-map)
    (.setDirections renderer (clj->js (-> directions :directions clj->js)))
    renderer))

(defn construct-polyline [index directions google-map]
  (let [polyline (js/google.maps.Polyline.
                   #js {:path #js []
                        :strokeColor (nth colors index)
                        :strokeOpacity 0.6
                        :geodesic true
                        :strokeWeight (nth widths index)})]
    (.setMap polyline google-map)
    (strict-map
      (fn [step]
        (strict-map
          (fn [segment]
            (-> polyline .getPath (.push (clj->js segment))))
          (:path step)))
      (-> directions :directions :routes (get 0) :legs (get 0) :steps))
    polyline))


(defn create-renderers [owner transit-directions show-vehicle-icons]
  (reduce
    (fn [renderers [index directions]]
      (if show-vehicle-icons
        (conj renderers (construct-renderer index directions (om/get-state owner :google-map)))
        (conj renderers (construct-polyline index directions (om/get-state owner :google-map)))))
    []
    (map vector (range) transit-directions)))

(defn gen-new-renderers [owner transit-directions show-vehicle-icons]
  (let [all-renderers (create-renderers owner transit-directions show-vehicle-icons)]
    (om/set-state! owner :all-renderers all-renderers)))

(defn create-markers [owner transit-journey]
  (reduce
    (fn [partial-markers [place-id location]]
      (conj
        partial-markers
        (js/google.maps.Marker.
          #js {:position location
               :map (om/get-state owner :google-map)
               :title (str place-id)
               :label (str (inc place-id))})))
    []
    (vec
      (map vector
           "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
           (conj
             (vec (map get-start-location transit-journey))
             (get-end-location (last transit-journey)))))))

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

(defn gen-new-markers [owner transit-journey]
  (let [all-markers (create-markers owner transit-journey)]
    (om/set-state! owner :all-markers all-markers)))

(defn valid-journey? [journey]
  (and (not (empty? (-> journey :start-place :coords))) (not (empty? (-> journey :end-place :coords)))))

(defn update-journey-plan [owner]
  (let [temporary-state (om/observe owner (models/temporary-state))
        {:keys [current-city journey transit-journey]} (om/get-props owner)
        waypoints (attractions/get-waypoints
                    (-> journey :waypoint-attraction-ids)
                    (-> current-city :attractions :data))]
    (when (valid-journey? journey) ;; just a safety check
      (.route
        (om/get-state owner :google-directions-service)
        #js {:origin (-> journey :start-place :coords clj->js)
             :destination (-> journey :end-place :coords clj->js)
             :waypoints (clj->js (map extract-goog-waypoint waypoints))
             :optimizeWaypoints true
             :travelMode (.. js/google -maps -TravelMode -DRIVING)}
        (fn [response status]
          (when (= status (.. js/google -maps -DirectionsStatus -OK))
            ;; Spawn threads a thread for each waypoint, and then collect the results
            (let [response-channel (chan)
                  response (js->clj response :keywordize-keys true) ]
              (get-transit-directions response-channel response waypoints (om/get-state owner :google-directions-service))
              (go
                (loop [msg-count (inc (count (-> response :request :waypoints)))
                       partial-directions []]
                  (if (> msg-count 0)
                    (recur (dec msg-count) (conj partial-directions (<! response-channel)))
                    (do
                      (om/update!
                        transit-journey
                        (make-transit-journey waypoints journey partial-directions))
                      (attractions/stop-loading-animation temporary-state))))))))))))

(defn fit-map-to-markers [google-map markers]
  (.fitBounds
    google-map
    (reduce
      (fn [partial-bounds marker]
        (if (some? (.getPosition marker))
          (.extend
            partial-bounds
            (.getPosition marker)))
        partial-bounds)
      (js/google.maps.LatLngBounds.)
      markers)))

(def throttled-update-journey-plan
  (util-funcs/throttle
    update-journey-plan
    (fn [owner]
      (let [{:keys [journey]} (om/get-props owner)]
        (if (valid-journey? journey) ;; just a safety check
          (* 2000 (-> journey :waypoint-attraction-ids keys count)) ;; wait 2000 ms per waypoint id
          0)))))

(defn attraction-map-view [[current-city journey transit-journey show-vehicle-icons] owner]
  (reify
    ;; initialize the google maps objects and store them as local state to the map view
    om/IDidMount
    (did-mount [_]
      (let [google-map-container (om/get-node owner)
            google-directions-service (js/google.maps.DirectionsService.)
            google-city-center (geojson->goog-latlng (-> current-city :city :data :center :coordinates))]
        (let [google-map (js/google.maps.Map.
                           google-map-container
                           #js {:center google-city-center
                                :overviewMapControl false
                                :mapTypeControl false
                                :minZoom 8
                                :maxZoom 20
                                :scrollwheel false
                                :streetViewControl false
                                :styles constants/map-style-arr
                                :zoom 12})]
          (om/set-state! owner :google-map google-map)
          (om/set-state! owner :google-directions-service google-directions-service)
          (gen-new-renderers owner transit-journey show-vehicle-icons)
          (gen-new-markers owner transit-journey)
          (if (valid-journey? journey)
            (fit-map-to-markers (om/get-state owner :google-map) (om/get-state owner :all-markers))))))

    ;; recompute the route upon journey update
    om/IDidUpdate
    (did-update [_ [prev-city prev-journey prev-transit-journey] _]
      (let [[city journey transit-journey] (om/get-props owner)]
        (when (valid-journey? journey)
          (when-not (= transit-journey prev-transit-journey)
            (remove-old-markers owner)
            (remove-old-renderers owner)
            (gen-new-renderers owner transit-journey show-vehicle-icons)
            (gen-new-markers owner transit-journey)
            (fit-map-to-markers (om/get-state owner :google-map) (om/get-state owner :all-markers))))
        (if-not (= (-> city :city :data :center :coordinates) (-> prev-city :city :data :center :coordinates))
          (.setCenter (om/get-state owner :google-map) (geojson->goog-latlng (-> current-city :city :data :center :coordinates))))))

    om/IRenderState
    (render-state [this state]
      (html [:div {:class "city-map-container"} "This is where the map should go"]))))

(defn attraction-map-legend-view [transit-journey owner]
  (reify
    om/IRender
    (render [_]
      (let [legend-tuples (map vector
                               "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                               (flatten (vector
                                          (:start_name (first transit-journey))
                                          (map #(:end_name %) transit-journey))))]
        (html [:table {:class "ui celled table"}
               [:thead
                [:th "Symbol"]
                [:th "Name"]]
               [:tbody
                (map #(html [:tr
                             [:td (get % 0)]
                             [:td (get % 1)]])
                     legend-tuples)]])))))

(defn city-view [{:keys [current-city journey transit-journey temporary-state]} owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [google-directions-service (js/google.maps.DirectionsService.)]
        (om/set-state! owner :google-directions-service google-directions-service))
      (if (valid-journey? journey)
        (throttled-update-journey-plan owner))
      (let [stickied-map (.find (js/$. (om/get-node owner)) ".stickied-map")]
        (if (.is stickied-map ":visible")
          (.sticky (.find (js/$. (om/get-node owner)) ".stickied-map")))))

    ;; A bit hacky, but register a listener on the root city component whose job is to sync
    ;; changes in the journey state to the url. Not sure of a better way to do it.
    ;; Perhaps better to put it in a different component...
    om/IDidUpdate
    (did-update [_ prev-props _]
      (let [stickied-map (.find (js/$. (om/get-node owner)) ".stickied-map")]
        (if (.is stickied-map ":visible")
          (.sticky (.find (js/$. (om/get-node owner)) ".stickied-map")))) ;; Hack to prevent stickied map from getting "stuck"
      (when-not (journey-same? journey (:journey prev-props))
        (if (valid-journey? journey)
          (throttled-update-journey-plan owner))
        (router/go-to-hash
          (http/encode-url-parameters
            (str "/city/" (-> current-city :city :data :id))
            (merge (if-not (empty? (-> journey :start-place :coords))
                     {"start-addr" (-> journey :start-place :address)
                      "start-lat" (gstring/format "%.6f" (-> journey :start-place :coords :lat))
                      "start-lng" (gstring/format "%.6f" (-> journey :start-place :coords :lng))})
                   (if-not (empty? (-> journey :end-place :coords))
                     {"end-addr" (-> journey :end-place :address)
                      "end-lat" (gstring/format "%.6f" (-> journey :end-place :coords :lat))
                      "end-lng" (gstring/format "%.6f" (-> journey :end-place :coords :lng))})
                   { "wp-ids[]" (clj->js (-> journey :waypoint-attraction-ids keys))})))))

    om/IRenderState
    (render-state [this _]
      (html [:div {:class "city-view"}
             ;; Header of the page
             [:div {:class "ui inverted vertical masthead center aligned segment"
                    :style {:background-image (str "url(\"" (-> current-city :city :data :cover_photo_url) "\")")}}
              [:div {:class "ui text container title"}
               [:h1 {:class "ui inverted header"} (str (-> current-city :city :data :name))]
               (om/build attractions-selector-view [current-city journey transit-journey])]]

             ;; Body of the page
             [:div {:class "ui centered grid"}
              [:div {:class "sixteen wide column row"}

               ;; The preview map only shows on computers
               [:div {:class "four wide column computer only preview-container"}
                [:div {:class "ui padded basic sticky stickied-map segment"}
                 [:div {:class "ui segment preview-directions"}
                  [:h3 "Route preview"]
                  [:div {:class (str "ui inverted dimmer " (if (is-loading? temporary-state) "active"))}
                   [:div {:class "ui text loader"} "Loading..."]]
                  (om/build attraction-map-view [current-city journey transit-journey false])
                  [:div {:class "ui basic centered segment"
                         :on-click #(animate-scroll-to-element (js/$. ".final-directions-header"))}
                   [:a {:class "ui red button"} "Final Route" ]]]]]

               ;; Adjust the width depending on whether on a computer vs a tablet/mobile device
               [:div {:class "twelve wide column computer only"}
                (om/build attractions/all-attractions-view [(-> current-city :attraction_categories :data)
                                                            (-> current-city :attractions :data)])]
               [:div {:class "sixteen wide column tablet mobile only"}
                (om/build attractions/all-attractions-view [(-> current-city :attraction_categories :data)
                                                            (-> current-city :attractions :data)])]]

              ;; Display the big map
              [:div {:class "ui fourteen wide column row final-directions-header"
                     :style {:padding-bottom 0}}
               [:h1 "Final directions"]]
              [:div {:class "ui fourteen wide column row basic segment final-directions"
                     :style {:margin-top 0}
                     }
               [:div {:class "three wide column"}
                (if (valid-journey? journey)
                  (om/build attraction-map-legend-view transit-journey)) ]
               [:div {:class "eleven wide column"}
                (om/build attraction-map-view [current-city journey transit-journey false])]]

              [:div {:class "ui fourteen wide column row basic segment final-transit-directions"}
               [:div {:class "fourteen wide column"}
                (if (valid-journey? journey)
                  (om/build transit-view transit-journey))]]]]))))
