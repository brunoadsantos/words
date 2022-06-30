(ns main.subs
  (:require [main.logic :as l]
            [re-frame.core :as rf]))

;; Extractors

(rf/reg-sub
 :game-mode
 (fn [db _]
   (:game-mode db)))

(rf/reg-sub
 :answer
 (fn [db _]
   (:answer db)))

(rf/reg-sub
 :final-answer
 (fn [db _]
   (:final-answer db)))

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
 :stats
 (fn [db _]
   (:stats db)))

(rf/reg-sub
 :prefs
 (fn [db _]
   (:prefs db)))

(rf/reg-sub
 :used-letters
 (fn [db _]
   (-> db (select-keys [:correct-letters :wrong-letters :misplaced-letters]))))

(rf/reg-sub
 :checked?
 (fn [db [_ n]]
   (get-in db [:attempts n :checked?])))

(rf/reg-sub
 :overlay-shown?
 (fn [db [_ overlay-id]]
   (get-in db [:overlays overlay-id])))

(rf/reg-sub
 :attempt-n
 (fn [db [_ n]]
   (get-in db [:attempts n :attempt])))

(rf/reg-sub
 :valid-attempt-n
 (fn [db [_ n]]
   (get-in db [:attempts n :valid-attempt])))

(rf/reg-sub
 :letter-results-n
 (fn [db [_ n]]
   (get-in db [:attempts n :letter-results])))

;; Materialized views

(rf/reg-sub
 :display-attempt-n
 (fn [[_ n] _]
   {:attempt-n (rf/subscribe [:attempt-n n])
    :valid-attempt-n (rf/subscribe [:valid-attempt-n n])
    :answer (rf/subscribe [:answer])})
 (fn [{:keys [attempt-n valid-attempt-n answer]} _]
   (let [answer-size (-> answer count)
         padding (->> attempt-n count (- answer-size))]
     (or (not-empty (map str valid-attempt-n))
         (concat attempt-n (repeat padding ""))))))

(rf/reg-sub
 :letter-details-n
 (fn [[_ n] _]
   {:attempt-n (rf/subscribe [:attempt-n n])
    :letter-results-n (rf/subscribe [:letter-results-n n])
    :current-attempt (rf/subscribe [:current-attempt])
    :answer (rf/subscribe [:answer])
    :last-added (rf/subscribe [:last-letter-position-added])})
 (fn [{:keys [attempt-n answer current-attempt last-added letter-results-n]} [_ n]]
   (let [answer-size (-> answer count)
         letter-results (or (seq letter-results-n) (repeat answer-size :unchecked))
         current-attempt-size (->> attempt-n (remove empty?) count)]
     (->> letter-results
          (map-indexed (fn [idx result]
                         {:idx idx
                          :result result
                          :current? (= idx current-attempt-size)
                          :current-row? (= n current-attempt)
                          :last-added? (= last-added [n idx])}))))))

(rf/reg-sub
 :attempt-row-n
 (fn [[_ n] _]
   {:display-attempt-n (rf/subscribe [:display-attempt-n n])
    :letter-details-n (rf/subscribe [:letter-details-n n])})
 (fn [{:keys [display-attempt-n letter-details-n]} _]
   (->> display-attempt-n
        (map (fn [l] {:letter l}))
        (map merge letter-details-n))))

(rf/reg-sub
 :game-over-info
 (fn [_ _]
   {:final-answer (rf/subscribe [:final-answer])
    :game-over? (rf/subscribe [:game-over?])
    :current-attempt (rf/subscribe [:current-attempt])})
 (fn [{:keys [final-answer game-over? current-attempt]} _]
   (merge game-over?
          {:final-answer final-answer
           :victory-attempt-number current-attempt})))

(rf/reg-sub
 :stats-info
 (fn [_ _]
   {:stats (rf/subscribe [:stats])
    :game-mode (rf/subscribe [:game-mode])
    :max-attempts (rf/subscribe [:max-attempts])})
 (fn [{:keys [stats game-mode max-attempts]} _]
   (let [max-amount (->> stats :attempts-to-win vals (apply max 1))
         distribution (for [i (range max-attempts)
                            :let [i (inc i)
                                  amount (get-in stats [:attempts-to-win i] 0)]
                            :when (pos-int? amount)]
                        {:attempt-number i
                         :number-of-wins amount
                         :fraction (/ amount max-amount)})]
     {:game-mode game-mode
      :total-games-played (:total-games-played stats 0)
      :total-wins (:total-wins stats 0)
      :attempts-distribution distribution})))

(def ^:private game-over-buttons
  [{:icon-name :leaderboard :code :show-stats :key :show-stats}
   {:icon-name :swap_horiz :code :switch-game-mode :key :switch-game-mode}
   {:icon-name :refresh :code :new-game :key :new-game}])

(def ^:private game-on-buttons
  [{:icon-name :backspace :code :delete :key :delete}
   {:icon-name :swap_horiz :code :switch-game-mode :key :switch-game-mode}
   {:icon-name :done :code :check :key :check}])

(rf/reg-sub
 :keyboard-info
 (fn [_ _]
   {:used-letters (rf/subscribe [:used-letters])
    :game-over? (rf/subscribe [:game-over?])})
 (fn [{:keys [used-letters game-over?]} _]
   (let [{:keys [game-over?]} game-over?
         ->buttons-props-fn (partial map #(l/letter->button-props % used-letters))
         rows [(->buttons-props-fn ["Q" "W" "E" "R" "T" "Y" "U" "I" "O" "P"])
               (->buttons-props-fn ["A" "S" "D" "F" "G" "H" "J" "K" "L"])
               (->buttons-props-fn ["Z" "X" "C" "V" "B" "N" "M"])
               (if game-over?
                 game-over-buttons
                 game-on-buttons)]
         rows-with-props (->> rows
                              (map-indexed (fn [idx row]
                                             {:key (str "button-row-" idx)
                                              :row row})))]
     {:rows rows-with-props})))
