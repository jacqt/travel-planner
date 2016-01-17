(ns travel-site.utils.util-funcs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan sliding-buffer <! poll! timeout]]))

(defn throttle [f debounce-length]
  (let [f-reqs (chan (sliding-buffer 1))]
    (go
      (loop []
        (let [request (<! f-reqs)]
          (apply f (:args request))
          (js/console.log debounce-length)
          (<! (timeout (if (fn? debounce-length)
                         (apply debounce-length (:args request))
                         debounce-length)))
          (recur))))

    (fn [& args]
      (put! f-reqs {:args args}))))
