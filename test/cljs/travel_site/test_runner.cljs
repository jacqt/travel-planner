(ns travel-site.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [travel-site.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'travel-site.core-test))
    0
    1))
