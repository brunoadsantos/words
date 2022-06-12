(ns main.effects
  (:require [alandipert.storage-atom :as sa]
            [clojure.string :as str]
            [main.view-helper :as vh]
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
              #(vh/add-class div (letter-result->reveal-class letter-result))
              (* idx 250)))
           (map vector divs letter-results))
          doall))))

(rf/reg-fx
 :reject-attempt
 (fn [{:keys [attempt-number attempt]}]
   (when (seq attempt)
     (vh/add-class (.getElementById js/document (str "row" attempt-number)) "reject-attempt"))))

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

;;; LOCAL STORAGE

(def local-storage (sa/local-storage (atom {}) :game-state))

(rf/reg-fx
 :save-game-state
 (fn [{:keys [game-mode game-state]}]
   (swap! local-storage assoc game-mode game-state)))

(rf/reg-cofx
 :saved-game-state
 (fn [cofx _]
   (assoc cofx :saved-game-state @local-storage)))
