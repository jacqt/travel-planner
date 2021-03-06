(ns travel-site.components.attractions
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [cljs.pprint :refer [pprint]]
            [exicon.semantic-ui]
            [travel-site.router :as router]
            [travel-site.utils.util-funcs :as util-funcs]
            [travel-site.utils.inputs :as inputs]
            [travel-site.utils.http :as http]
            [travel-site.models :as models]))

(defn trigger-loading-animation [temporary-state]
  (om/update! temporary-state :is-loading true))

(defn stop-loading-animation [temporary-state]
  (om/update! temporary-state :is-loading false))

(defn attraction-selected? [attraction waypoint-attraction-ids]
  (contains? waypoint-attraction-ids (:id attraction)))

(defn add-waypoint [attraction]
  (let [waypoint-attraction-ids (models/waypoint-attraction-ids)]
    (om/update! waypoint-attraction-ids (assoc waypoint-attraction-ids (:id attraction) true))))

(defn remove-waypoint [attraction]
  (let [waypoint-attraction-ids (models/waypoint-attraction-ids)]
    (om/update! waypoint-attraction-ids (dissoc waypoint-attraction-ids (:id attraction)))))

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
      (keys waypoint-attraction-ids))))


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
    om/IInitState
    (init-state [_]
      {:on-click (util-funcs/throttle
                   (fn [waypoint-attraction-ids temporary-state]
                     (trigger-loading-animation temporary-state)
                     (.transition (js/$. (om/get-node owner)) "bounce")
                     (if (attraction-selected? attraction waypoint-attraction-ids)
                       (remove-waypoint attraction)
                       (add-waypoint attraction)))
                   1000)})

    om/IRenderState
    (render-state [this state]
      (let [waypoint-attraction-ids (om/observe owner (models/waypoint-attraction-ids))
            temporary-state (om/observe owner (models/temporary-state))]
        (html [:div {:class "ui fluid centered card attraction-card-view"
                     :on-click #((:on-click state)
                                   waypoint-attraction-ids
                                   temporary-state)}
               [:div {:class "image"
                      :style {:background-image (str "url( " (:image_url attraction) " )")}}]
               [:div {:class "content"}
                [:div {:class "header"} (:name attraction)]
                [:div {:class "description"} (:description attraction)]]
               (if (attraction-selected? attraction waypoint-attraction-ids)
                 [:div {:class "ui bottom attached green button"}
                  [:i {:class "check outline icon"}]
                  "Added!" ]
                 [:div {:class "ui bottom attached blue button"}
                  [:i {:class "plus outline icon"}]
                  "Add to journey"])])))))

(defn category-view [[category attractions] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "ui padded basic segment"}
             [:h1 (:name category)]
             [:div {:class "ui link three stackable cards category-view"}
             (om/build-all attraction-card-view attractions)]]))))

(defn all-attractions-view [[attraction-categories attractions] owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "all-attractions-view"}
             (map
               (fn [category]
                 (om/build category-view [category (filter #(= (:id category) (:category_id %)) attractions)]))
               attraction-categories)]))))

