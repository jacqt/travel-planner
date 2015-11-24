(ns travel-site.components.dashboard
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.utils.auth :as auth]
            [travel-site.utils.http :as http]))


(defn logout [credentials]
  (auth/clear-credentials)
  (om/update! credentials (auth/get-credentials)))

(defn city-button [city owner]
  (reify
    om/IRender
    (render [_]
      (html [:a {:class "ui button"
                 :href (str "#/city/" (:id city))}
             (:city-name city)]))))

(defn dashboard-view [{:keys [cities credentials]} owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div {:class "dashboard-panel"}
             [:div {:class "dashboard-welcome"}
              [:h1 "Welcome to your travel planner!"] ]
             [:div {:class "dashboard-content"}
              (om/build-all city-button cities)
              [:button {:class "ui button"
                        :on-click #(logout credentials)}
               "Click here to logout"]
              [:input {:class "dashboard-input"
                       :placeholder "Enter a name here!"}]]]))))
