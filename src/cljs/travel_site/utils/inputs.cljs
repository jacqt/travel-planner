(ns travel-site.utils.inputs
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [exicon.semantic-ui]
            [sablono.core :refer-macros [html]]
            [cljsjs.moment]
            [cljsjs.jquery]
            [cljsjs.jquery-ui]))

;;; These components require jquery, jquery-ui

;; Generic component for an input box with a two-way databinding
;; between a property of the state and the value of the input
;;
;; Works by passing into the component a vector of length two
;; with the first being a cursor to the parent of the input, and the second
;; being a map with the keys
;;  :className        (maps to the classname on the input created)
;;  :edit-key         (the key in the parent-state to the text)
;;  :placeholder-text (the placeholder text in the creatd input)

(defn handle-change [e data edit-key]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn editable-input [[parent-state {:keys [className edit-key placeholder-text]}]  owner]
  {:pre [(some? parent-state)]}
  (reify
    om/IRenderState
    (render-state [_ _]
      (dom/input
        #js {:className className
             :placeholder placeholder-text
             :onChange #(handle-change % parent-state edit-key)
             :value (edit-key parent-state)
             :type "text" }))))


;; Generic component for using google places autocomplete input
;; Same API as the editable-input component above
(defn address-autocomplete-input [[parent-state {:keys [className edit-key placeholder-text coords-key]}] owner] 
  {:pre [(some? parent-state)]}
  (reify
    om/IDidMount
    (did-mount [_]
      (let [address-input (om/get-node owner)]
        (let [autocomplete (js/google.maps.places.Autocomplete. address-input)]
          (.addListener
            autocomplete
            "place_changed"
            (fn []
              (if (some? coords-key)
                (let [place (.getPlace autocomplete)]
                  (om/transact! parent-state coords-key (fn [_] {:lat (.lat (.. place -geometry -location))
                                                                 :lng (.lng (.. place -geometry -location))}))))
              (om/transact! parent-state edit-key (fn [_] (.-value address-input))))))))
    om/IRenderState
    (render-state [_ _]
      (om/build editable-input [parent-state {:className className
                                              :edit-key edit-key
                                              :placeholder-text placeholder-text }]))))

(defn clean-timestring []
  (let [moment-obj (js/moment.)]
    (.seconds moment-obj 0)
    (.milliseconds moment-obj 0)
    (.toISOString moment-obj)))

(defn update-timestring-date [timestring date]
  {:pre [(some? date)]}
  (if (not (.isValid (js/moment. timestring)))
    (let [timestring (clean-timestring)]
      (update-timestring-date timestring date))
    (do
      (let [moment-obj (js/moment. timestring)
            parsed-date (js/moment. date "MM/DD/YYYY")]
        (.date moment-obj (.date parsed-date))
        (.month moment-obj (.month parsed-date))
        (.year moment-obj (.year parsed-date))
        (-> moment-obj .toISOString)))))

(defn update-timestring-time [timestring time]
  {:pre [(some? time)]}
  (if (not (.isValid (js/moment. timestring)))
    (let [timestring (clean-timestring)]
      (update-timestring-time timestring time))
    (do
      (let [moment-obj (js/moment. timestring)
            parsed-time (js/moment. time "h:mm A")]
        (.hour moment-obj (.hour parsed-time))
        (.minute moment-obj (.minute parsed-time))
        (-> moment-obj .toISOString)))))

(defn extract-date [timestring]
  (let [moment-obj (js/moment. timestring)]
    (if (.isValid moment-obj)
      (.format moment-obj "MM/DD/YYYY")
      "")))

(defn extract-time [timestring]
  (let [moment-obj (js/moment. timestring)]
    (if (.isValid moment-obj)
      (.format moment-obj "h:mm A")
      "")))

(defn on-time-change [parent-state edit-key  update-timestring-func update-value]
  (om/transact!
    parent-state edit-key
    (fn [_] (update-timestring-func (edit-key parent-state) update-value))))

;; Generic datepicker component that uses jquery-ui for the calendar view
;; Same API as the editable-input component above along with the min-date parameter
(defn datepicker-input [[parent-state {:keys [className min-date placeholder-text edit-key]}] owner]
  {:pre [(some? parent-state)]}
  (reify
    om/IDidMount
    (did-mount [_]
      (->
        owner
        om/get-node
        js/$.
        (.datepicker
          #js {:minDate min-date
               :onSelect (partial on-time-change parent-state edit-key update-timestring-date)})))

    om/IDidUpdate
    (did-update [_ _ _]
      (->
        owner
        om/get-node
        js/$.
        (.datepicker "option" "minDate" min-date)))

    om/IRenderState
    (render-state [_ _]
      (dom/input
        #js {:className className
             :placeholder placeholder-text
             :value (-> parent-state edit-key extract-date)
             :type "text" }))))
