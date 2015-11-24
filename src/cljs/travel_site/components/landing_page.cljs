(ns travel-site.components.landing-page
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.components.login-signup :as login-signup]))

(defn page-content-view [{:keys [credentials]} this]
  (reify om/IRenderState
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
               [:h1 {:class "ui inverted header"} "A Title!"]
               [:h2 "Your wonder subtitle goes here"]
               (om/build login-signup/login-signup-view credentials)]]]))))

(defn landing-page-view [state owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (html [:div
             (om/build page-content-view state)]))))
