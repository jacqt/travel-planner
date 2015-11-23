(ns travel-site.components.event
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.utils.http :as http]))

(defn event-view [{:keys [event]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "dashboard-panel"}
        (dom/h1
          #js {}
          "Event page here!")
        (dom/h2
          #js {}
          (str "Event Id: " (event :id)))))))
