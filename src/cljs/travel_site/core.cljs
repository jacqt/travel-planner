(ns travel-site.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljsjs.jquery]
            [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.router :as router]
            [travel-site.index :as index]
            [travel-site.models :as models]))


(defn main []
  (router/route-app models/app-state)
  (secretary/dispatch!
    (.substring (.. js/window -location -hash) 1))
  (om/root
    index/index-view
    models/app-state
    {:target (js/document.getElementById "app")}))
