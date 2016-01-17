(ns travel-site.components.dashboard
  (:require [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.utils.auth :as auth]
            [travel-site.utils.http :as http]))

(def royal-park-url "#/city/1?start-place=%7B%22address%22%3A%22Paddington%20Station%2C%20London%2C%20United%20Kingdom%22%2C%22coords%22%3A%7B%22lat%22%3A51.51651769999999%2C%22lng%22%3A-0.1766506000000163%7D%7D&end-place=%7B%22address%22%3A%22Paddington%20Station%2C%20London%2C%20United%20Kingdom%22%2C%22coords%22%3A%7B%22lat%22%3A51.51651769999999%2C%22lng%22%3A-0.1766506000000163%7D%7D&waypoint-attraction-ids[]=18&waypoint-attraction-ids[]=12&waypoint-attraction-ids[]=1&waypoint-attraction-ids[]=14")

(def historic-site-url "#/city/1?start-place=%7B%22address%22%3A%22Paddington%20Station%2C%20London%2C%20United%20Kingdom%22%2C%22coords%22%3A%7B%22lat%22%3A51.51651769999999%2C%22lng%22%3A-0.1766506000000163%7D%7D&end-place=%7B%22address%22%3A%22Paddington%20Station%2C%20London%2C%20United%20Kingdom%22%2C%22coords%22%3A%7B%22lat%22%3A51.51651769999999%2C%22lng%22%3A-0.1766506000000163%7D%7D&waypoint-attraction-ids[]=16&waypoint-attraction-ids[]=9&waypoint-attraction-ids[]=10&waypoint-attraction-ids[]=4&waypoint-attraction-ids[]=3&waypoint-attraction-ids[]=2")

(defn logout [credentials]
  (auth/clear-credentials)
  (om/update! credentials (auth/get-credentials)))

(defn city-button [city owner]
  (reify
    om/IRender
    (render [_]
      (html [:a {:class "ui large inverted button"
                 :href (str "#/city/" (:id city))}
             (:name city)
             [:i {:class "ui angle right icon"}]]))))

(defn dashboard-view [{:keys [cities credentials]} owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (http/get-all-cities
        (fn [{:keys [data]}]
          (om/update! cities data))))

    om/IRenderState
    (render-state [this _]
      (html [:div {:class "pusher"}
             ;; Header
             [:div {:class "ui inverted vertical masthead center aligned segment landing-page"}
              [:div {:class "ui text container title"}
               [:h1 {:class "ui inverted header"} "Fiberboard"]
               [:h2 "Plan your journey in..."]
               (om/build-all city-button cities)]]

             ;; Body of the page
             [:div {:class "ui vertical stripe segment"}
              [:div {:class "ui middle aligned stackable grid container"}
               [:div {:class "row"}
                [:div {:class "eight wide column" }
                 [:h1 {:class "ui header"}
                  "Have a free day to spend in London?"]
                 [:div {:class "ui list"}
                  [:a {:href royal-park-url :class "item"} "Why not take a look at some of London's Royal Parks?"]
                  [:a {:href historic-site-url :class "item"} "Or visit the historic sites in London"]]
                 [:h2 {:class "ui header"}
                  "Planning your day journey made easy"]
                 [:p "ayee lmao"]]
                [:div {:class "six wide right floated column"}
                 [:img {:src "http://semantic-ui.com/examples/assets/images/wireframe/white-image.png"
                        :class "ui large bordered rounded image"}]]]
               [:div {:class "row"}
                [:div {:class "center aligned column"}
                 [:a {:class "ui huge button"} "Check them out"]]]]]

             ;; Footer of the page
             [:div {:class "ui inverted vertical footer segment"}
              [:div {:class "ui container"}
               [:div {:class "ui stackable inverted divided equal height stackable grid"}
                [:div {:class "three wide column"}
                 [:h4 {:class "ui inverted header"} "About"]
                 [:div {:class "ui inverted link list"}
                  [:a {:href "#" :class "item"} "Item 1"]
                  [:a {:href "#" :class "item"} "Item 2"]
                  [:a {:href "#" :class "item"} "Item 3"]
                  [:a {:href "#" :class "item"} "Item 4"]]]
                [:div {:class "three wide column"}
                 [:h4 {:class "ui inverted header"} "Services"]
                 [:div {:class "ui inverted link list"}
                  [:a {:href "#" :class "item"} "Item 1"]
                  [:a {:href "#" :class "item"} "Item 2"]
                  [:a {:href "#" :class "item"} "Item 3"]
                  [:a {:href "#" :class "item"} "Item 4"]]]
                [:div {:class "seven wide column"}
                 [:h4 {:class "ui inverted header"} "Footer Header"]
                 [:p "Extra space etc. etc etc."]]]]]]))))
