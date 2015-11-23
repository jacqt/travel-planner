(ns travel-site.utils.auth
  (:require [goog.net.cookies :as cookies]))

(defn set-credentials [{:keys [facebook-id auth-token]}]
  (println (str "Trying to set credential cookies now... " facebook-id ", " auth-token))
  (.set goog.net.cookies "facebook-id" facebook-id -1)
  (.set goog.net.cookies "auth-token" auth-token -1))

(defn clear-credentials []
  (println "Clearing all cookies now...")
  (.clear goog.net.cookies))

(defn get-credentials []
  (println "Retrieving credentials from cookies")
  (let [facebook-id (goog.net.cookies.get "facebook-id")
        auth-token  (goog.net.cookies.get "auth-token")]
    (if (or (= facebook-id nil) (= auth-token nil))
      {}
      {:facebook-id facebook-id
       :auth-token auth-token})))
