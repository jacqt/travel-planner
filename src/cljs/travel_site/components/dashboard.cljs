(ns travel-site.components.dashboard
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.utils.auth :as auth]
            [travel-site.utils.http :as http]))


(defn logout [credentials]
  (auth/clear-credentials)
  (om/update! credentials (auth/get-credentials)))

(defn dashboard-view [{:keys [credentials]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "dashboard-panel"}
        (dom/div
          #js {:className "dashboard-welcome"}
          (dom/h1
            #js {}
            "Welcome to FriendBnb!"))
        (dom/div
          #js {:className "dashboard-content"}
          (dom/button
            #js {:className "ui button"}
            "Click here to do random stuff")
          (dom/button
            #js {:className "ui button"
                 :onClick (fn [e] (logout credentials))}
            "Click here to logout")
          (dom/input
            #js {:className "dashboard-input"
                 :placeholder "Enter your friend's name!"}))))))
