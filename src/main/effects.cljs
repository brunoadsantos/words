(ns main.effects
  (:require [alandipert.storage-atom :as sa]
            [clojure.string :as str]
            [main.utils :as utils]
            [re-frame.core :as rf]))

(def letter-result->reveal-class
  {:correct "reveal-correct"
   :misplaced "reveal-misplaced"
   :wrong "reveal-wrong"})

(rf/reg-fx
 :reveal-attempt
 (fn [{:keys [attempt-number letter-results]}]
   (let [divs (.querySelectorAll js/document (str "#row" attempt-number " div"))]
     (->> (map-indexed
           (fn [idx [div letter-result]]
             (js/setTimeout
              #(utils/add-class div (letter-result->reveal-class letter-result))
              (* idx 250)))
           (map vector divs letter-results))
          doall))))

(def ^:private pattern->js-pattern
  {:key-input (clj->js [10])
   :reject-attempt (clj->js [150 100 150 100 150])})

(rf/reg-fx
 :reject-attempt
 (fn [{:keys [attempt-number attempt vibrate?]}]
   (when (seq attempt)
     (when (and vibrate? js/navigator.vibrate)
       (js/navigator.vibrate (pattern->js-pattern :reject-attempt)))
     (utils/add-class (.getElementById js/document (str "row" attempt-number)) "reject-attempt"))))

(rf/reg-cofx
 :game-mode-from-url
 (fn [cofx _]
   (let [location-hash (-> js/location
                           .-hash
                           str
                           (subs 1)
                           str/lower-case)]
     (assoc cofx
            :game-mode-from-url (if (= "capitu" location-hash) :capitu :bento)))))

(rf/reg-fx
 :vibrate
 (fn [{:keys [pattern vibrate?]}]
   (when (and vibrate? js/navigator.vibrate)
     (js/navigator.vibrate (pattern->js-pattern pattern)))))

;;; LOCAL STORAGE

(def local-storage (sa/local-storage (atom {}) :game-state))

(rf/reg-fx
 :save-prefs
 (fn [{:keys [prefs]}]
   (swap! local-storage assoc :prefs prefs)))

(defn ^:private persistable-state [db]
  (dissoc db :word-map :valid-attempts :game-mode :overlays :prefs))

(rf/reg-fx
 :save-game-state
 (fn [{:keys [game-mode game-state]}]
   (swap! local-storage assoc game-mode (persistable-state game-state))))

(rf/reg-cofx
 :saved-game-state
 (fn [cofx _]
   (assoc cofx :saved-game-state @local-storage)))
