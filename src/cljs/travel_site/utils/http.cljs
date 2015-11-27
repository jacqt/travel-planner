(ns travel-site.utils.http
  (:require [travel-site.utils.auth :as auth]
            [goog.uri.utils :as uri-utils]
            [goog.net.XhrIo :as net-xhrio]))

(enable-console-print!)

(def BASE_URL "http://fiberboard.notifsta.com/api")
(def CITIES_URL (str BASE_URL "/cities"))

; parses goog.net.XhrIo response to a json
(defn parse-xhrio-response [success-callback fail-callback]
  (fn [response]
    (let [target (aget response "target")]
      (if (.isSuccess target)
        (let [json (.getResponseJson target)]
          (success-callback (js->clj json :keywordize-keys true)))
        (let [error (.getLastError target)]
          (fail-callback (js->clj error :keywordize-keys true)))))))

(defn encode-url-parameters [base-url url-params]
  (reduce
    (fn [partial-url param-key]
      (.appendParams
        goog.uri.utils
        partial-url
        (name param-key)
        (url-params param-key)))
    base-url
    (keys url-params)))

; wraps goog.net.XhrIo library in a simpler function xhr
(defn xhr [{:keys [method base-url url-params on-complete on-error]}]
  (.send
    goog.net.XhrIo
    (encode-url-parameters base-url url-params)
    (parse-xhrio-response on-complete on-error)
    method))

(defn get-all-cities [on-complete]
  (xhr {:method "GET"
        :base-url CITIES_URL
        :url-params {}
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get data for all cities"))}))

(defn get-city [city-id on-complete]
  (xhr {:method "GET"
        :base-url (str CITIES_URL "/" city-id)
        :url-params {}
        :on-complete on-complete
        :on-error (fn [error] (println "[LOG] Failed to get data for current city "))}))
