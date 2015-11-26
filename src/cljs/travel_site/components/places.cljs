(ns travel-site.components.places
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]))

(defn add-waypoint [place]
  (let [waypoint-place-ids (models/waypoint-place-ids)]
    (om/update! waypoint-place-ids (conj waypoint-place-ids (:id place)))))

(defn extract-waypoint [place]
  {:location (:location place) 
   :stopover true })

(defn get-waypoints [waypoint-place-ids place-list]
  (let [id-to-place (reduce (fn [id-to-place place] (assoc id-to-place (:id place) place)) {} place-list)]
    (reduce
      (fn [waypoints place-id]
        (conj
          waypoints
          (extract-waypoint (get id-to-place place-id))))
      []
      waypoint-place-ids)))


(defn place-view [place owner]
  (reify
    om/IRender
    (render [_]
      (html [:div
             (:place-name place)
             [:button {:class "ui button"}
              "Add"] ]))))

(defn place-category-view [waypoint owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment place-category-view"}
             [:h4 (:location waypoint)]]))))

(defn waypoints-selector-view [current-city owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html [:div
             [:h2 "Waypoints"]
             (om/build-all place-category-view (get-waypoints (-> current-city :journey :waypoint-place-ids clj->js) (:places current-city)))]))))

(defn place-card-view [place owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "ui fluid centered card place-card-view"
                   :on-click #(add-waypoint place)}
             [:div {:class "image"
                    :style {:background-image "url(https://notifsta.s3.amazonaws.com/St%20johns%20College.jpg)"}}]
             [:div {:class "content"}
              [:div {:class "header"} (:place-name place)]
              [:div {:class "description"} (:location place)]]]))))

(defn category-view [[category places] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui link four stackable cards category-view"}
             (om/build-all place-card-view places)]))))

(defn all-places-view [[place-categories places] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment all-places-view"}
             (map
               (fn [category]
                 (om/build category-view [category (filter #(= (:id category) (:category-id %)) places)]))
               place-categories)]))))

