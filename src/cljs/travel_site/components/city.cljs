(ns travel-site.components.city
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
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
      (html [:div
             [:h1 (:category-name category)]
             (om/build-all place-view (:places category))]))))

(defn city-view [{:keys [current-city]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "dashboard-panel"}
             [:pre (with-out-str (print current-city))]
             [:h2 (str "City id: " (:id current-city))]
             [:div (om/build-all place-category-view (:place-categories current-city))]]))))
