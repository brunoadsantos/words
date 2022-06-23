(ns main.ga-effects
  (:require [re-frame.core :as rf]))

(rf/reg-fx
 :ga-event
 (fn [{:keys [event-name event-params]}]
   (when js/gtag
     (js/gtag "event" event-name (clj->js event-params)))))
