(ns travel-site.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljsjs.jquery]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.components.dashboard :as dashboard]
            [travel-site.components.city :as city]
            [travel-site.utils.http :as http]
            [travel-site.utils.auth :as auth]))

(defn index-view [state owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (case (@state :route)
        "home" (om/build dashboard/dashboard-view state)
        "city" (om/build city/city-view state)
        (comment default) (om/build dashboard/dashboard-view state)))))
