(ns main.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 :game-mode
 (fn [db _]
   (:game-mode db)))

(rf/reg-sub
 :answer
 (fn [db _]
   (select-keys db [:answer :final-answer])))

(rf/reg-sub
 :revealing?
 (fn [db _]
   (:revealing? db)))

(rf/reg-sub
 :max-attempts
 (fn [db _]
   (:max-attempts db)))

(rf/reg-sub
 :current-attempt
 (fn [db _]
   (:attempt-number db)))

(rf/reg-sub
 :last-letter-position-added
 (fn [db _]
   (:last-letter-position-added db)))

(rf/reg-sub
 :game-over?
 (fn [db _]
   (select-keys db [:game-over? :success?])))

(rf/reg-sub
 :used-letters
 (fn [db _]
   (-> db (select-keys [:correct-letters :wrong-letters :misplaced-letters]))))

(rf/reg-sub
 :checked?
 (fn [db [_ n]]
   (get-in db [:attempts n :checked?])))

(rf/reg-sub
 :attempt
 (fn [db [_ n]]
   (let [answer-size (-> db :answer count)
         attempt (get-in db [:attempts n :attempt] [])
         valid-attempt (get-in db [:attempts n :valid-attempt])
         padding (->> attempt count (- answer-size))]
     {:attempt (concat attempt (repeat padding ""))
      :valid-attempt valid-attempt})))

(rf/reg-sub
 :letter-results
 (fn [db [_ n]]
   (let [answer-size (-> db :answer count)
         letter-results (get-in db [:attempts n :letter-results] [])]
     (if (seq letter-results)
       letter-results
       (repeat answer-size :unchecked)))))

(rf/reg-sub
 :overlay-shown?
 (fn [db [_ overlay-id]]
   (get-in db [:overlays overlay-id])))
