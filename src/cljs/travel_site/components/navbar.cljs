(ns travel-site.components.navbar
  (:require [om.core :as om :include-macros true]
            [cljsjs.jquery]
            [sablono.core :refer-macros [html]]
            [exicon.semantic-ui]
            [travel-site.router :as router]
            ))

(defn navbar-view [app-state owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (.dropdown
        (.find (js/$. (om/get-node owner)) ".ui.dropdown")
        #js {:onChange
               (fn [new-city-id]
                 (router/go-to-hash (str "/city/" new-city-id)))}
        ))

    om/IRender
    (render [this]
      (html [:div {:class "computer tablet only row"}
             [:div {:class "ui inverted menu navbar"}
              [:a {:class "brand item" :href "#/"}
               "Home"]
              [:div {:class "ui search selection dropdown"}
               [:input {:type "hidden" :name "city"
                        :on-change #(js/console.log %)
                        }]
               [:i {:class "dropdown icon"}]
               [:div {:class "default text"} "Plan your trip in..."]
               [:div {:class "menu"}
                [:div {:class "item" :data-value "1"} "London"]
                [:div {:class "item" :data-value "2"} "Paris"]
                [:div {:class "item" :data-value "3"} "Oxford"]
                ]
               ]
              ]
             ]
        ))))

