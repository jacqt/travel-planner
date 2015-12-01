(ns travel-site.utils.util-funcs
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan sliding-buffer <! poll! timeout]]))

(defn throttle [f debounce-length]
  (let [f-reqs (chan (sliding-buffer 1))]
    (go
      (loop []
        (let [request (<! f-reqs)]
          (apply f (:args request))
          (<! (timeout debounce-length))
          (recur))))

    (fn [& args]
      (put! f-reqs {:args args}))))