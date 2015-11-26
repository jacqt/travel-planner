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
      (html [:div {:class "pusher"}
             [:div {:class "ui inverted vertical masthead center aligned segment"}
              [:div {:class "ui container"}
               [:div {:class "ui large secondary inverted pointing menu"}
                [:a {:class "active item"} "Home"]
                [:a {:class "item"} "Route1"]
                [:a {:class "item"} "Route2"]
                [:div {:class "right item"}
                 [:a {:class "ui inverted button"} "Log in"]
                 [:a {:class "ui inverted button"} "Sign up"]]]]
              [:div {:class "ui text container"}
               [:h1 {:class "ui inverted header"} "Fiberboard"]
               [:h2 "Plan your trip to..."]
               (om/build-all city-button cities)]] ]))))
