(ns main.core
  (:require [clojure.string :as str]
            [main.effects]
            [main.events]
            [main.ga-effects]
            [main.subs]
            [main.view-helper :as vh]
            [main.views]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]))

(rf/dispatch-sync [:new-game])

(defn letter-revealed [^js/Event event]
  (let [letter-idx (-> event
                       .-target
                       (.getAttribute "data-letter-idx")
                       (js/parseInt 10))]
    (rf/dispatch [:letter-revealed letter-idx])))

(defn on-animation-ended [event]
  (case (.-animationName event)
    "reject-attempt" (-> event .-target (vh/remove-class "reject-attempt"))
    "reveal-letter" (letter-revealed event)
    nil)

  (.preventDefault event))

(defn keydown-listener-fn [event]
  (let [key-code (.-keyCode event)]
    (cond
      (<= 65 key-code 90)
      (rf/dispatch [:key-input (str/upper-case (.-key event))])

      (= 13 key-code)
      (rf/dispatch [:key-input :check])

      (#{8 46} key-code)
      (rf/dispatch [:key-input :delete])))

  (.preventDefault event))

(defn render []
  (rdom/render
   [main.views/app]
   (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render! []
  (rf/clear-subscription-cache!)
  (render))

(defn register-service-worker []
  (when-let [sw (.-serviceWorker js/navigator)]
    (js/console.info "Registering Service Worker")
    (-> sw
        (.register "/worker.js" #js {:scope "/"})
        (.then js/console.info))))

(defn ^:export init []
  (register-service-worker)

  (.removeEventListener js/document "animationend" on-animation-ended)
  (.addEventListener js/document "animationend" on-animation-ended)

  (.removeEventListener js/document "keyup" keydown-listener-fn)
  (.addEventListener js/document "keyup" keydown-listener-fn)

  (render))
