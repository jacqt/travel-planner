(ns travel-site.components.login-signup
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]
            [travel-site.utils.auth :as auth]
            [travel-site.utils.http :as http]))

(defn login-to-facebook [credentials]
  (js/FB.login
    (fn [facebook-response]
      (js/FB.api
        "/me?fields=id,name,email"
        (fn [facebook-profile]
          (js/console.log facebook-profile)
          (http/login
            (.. facebook-response -authResponse -userID)
            (.. facebook-response -authResponse -accessToken)
            (. facebook-profile -email)
            (fn [response]
              (let [new-credentials (:data response)]
                (auth/set-credentials {:facebook-id (:facebook_id new-credentials)
                                       :auth-token (:authentication_token new-credentials)})
                (om/update! credentials (auth/get-credentials))))))))
    #js{"scope" "email"}))

(defn login-signup-view [credentials owner]
  (reify
    om/IRenderState
    (render-state [this _]
      (dom/div
        #js {:className "login-panel"}
        (dom/div
          #js {:className "login-component"}
          (dom/button
            #js {:onClick (fn [e] (login-to-facebook credentials))
                 :className "ui primary button"}
            (dom/i
              #js {:className "facebook icon"})
            "Login or signup with Facebook"))))))
