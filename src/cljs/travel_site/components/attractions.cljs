(ns travel-site.components.attractions
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [travel-site.router :as router]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]))

(defn add-waypoint [attraction]
  (let [waypoint-attraction-ids (models/waypoint-attraction-ids)]
    (if-not (contains? (into #{} waypoint-attraction-ids) (:id attraction))
      (om/update! waypoint-attraction-ids (conj waypoint-attraction-ids (:id attraction))))))

(defn remove-waypoint [attraction]
  (let [waypoint-attraction-ids (models/waypoint-attraction-ids)]
    (om/update! waypoint-attraction-ids (vec (filter #(not (= % (:id attraction))) waypoint-attraction-ids)))))

(defn attractions->id-to-attraction [attraction-list]
  (reduce (fn [id-to-attraction attraction] (assoc id-to-attraction (:id attraction) attraction)) {} attraction-list))

(defn get-waypoints [waypoint-attraction-ids attraction-list]
  (let [id-to-attraction (attractions->id-to-attraction attraction-list)]
    (reduce
      (fn [waypoints attraction-id]
        (conj
          waypoints
          (get id-to-attraction attraction-id)))
      []
      waypoint-attraction-ids)))


(defn attraction-editor-view [waypoint owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment attraction-editor-view"}
             [:h4 (:name waypoint)]
             [:div
              [:p (:description waypoint)]
              [:button {:class "ui basic button"
                        :on-click #(remove-waypoint waypoint)}
               "Remove"] ]
             ]))))

(defn waypoints-selector-view [[current-city journey] owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (html [:div
             [:h2 "Waypoints"]
             (om/build-all
               attraction-editor-view
               (get-waypoints
                 (-> journey :waypoint-attraction-ids)
                 (-> current-city :attractions :data)))]))))

(defn attraction-card-view [attraction owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "ui fluid centered card attraction-card-view"
                   :on-click #(add-waypoint attraction)}
             [:div {:class "image"
                    :style {:background-image (str "url( " (:image_url attraction) " )")}}]
             [:div {:class "content"}
              [:div {:class "header"} (:name attraction)]
              [:div {:class "description"} (:description attraction)]]]))))

(defn category-view [[category attractions] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div
             [:h1 (:name category)]
             [:div {:class "ui link four stackable cards category-view"}
             (om/build-all attraction-card-view attractions)]]))))

(defn all-attractions-view [[attraction-categories attractions] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui segment all-attractions-view"}
             (map
               (fn [category]
                 (om/build category-view [category (filter #(= (:id category) (:category_id %)) attractions)]))
               attraction-categories)]))))

