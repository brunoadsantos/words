(ns main.subs
  (:require [main.logic :as l]
            [re-frame.core :as rf]))

;; Extractors

(defn ^:private db->sub [kw]
  (rf/reg-sub kw :-> kw))

(db->sub :game-mode)

(db->sub :answer)

(db->sub :final-answer)

(db->sub :revealing?)

(db->sub :max-attempts)

(db->sub :last-letter-position-added)

(db->sub :stats)

(db->sub :prefs)

(rf/reg-sub
 :current-attempt :-> :attempt-number)

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
   [(rf/subscribe [:attempt-n n])
    (rf/subscribe [:valid-attempt-n n])
    (rf/subscribe [:answer])])
 (fn [[attempt-n valid-attempt-n answer] _]
   (let [answer-size (-> answer count)
         padding (->> attempt-n count (- answer-size))]
     (or (not-empty (map str valid-attempt-n))
         (concat attempt-n (repeat padding ""))))))

(rf/reg-sub
 :letter-details-n
 (fn [[_ n] _]
   [(rf/subscribe [:attempt-n n])
    (rf/subscribe [:letter-results-n n])
    (rf/subscribe [:current-attempt])
    (rf/subscribe [:answer])
    (rf/subscribe [:last-letter-position-added])])
 (fn [[attempt-n letter-results-n current-attempt answer last-added] [_ n]]
   (let [answer-size (-> answer count)
         letter-results (or (seq letter-results-n) (repeat answer-size :unchecked))
         current-attempt-size (->> attempt-n (remove empty?) count)]
     (->> letter-results
          (map-indexed (fn [idx result]
                         {:idx idx
                          :key (str idx)
                          :result result
                          :current? (= idx current-attempt-size)
                          :current-row? (= n current-attempt)
                          :last-added? (= last-added [n idx])}))))))

(rf/reg-sub
 :attempt-row-n
 (fn [[_ n] _]
   [(rf/subscribe [:display-attempt-n n])
    (rf/subscribe [:letter-details-n n])])
 (fn [[display-attempt-n letter-details-n] _]
   (->> display-attempt-n
        (map (fn [l] {:letter l}))
        (map merge letter-details-n))))

(defn ^:private attempt-number->message [n]
  (case n
    0 "Ótimo chute!"
    1 "Excelente!"
    2 "Impressionante!"
    3 "Ótimo!"
    4 "Muito bem!"
    "Ufa!"))

(rf/reg-sub
 :game-over-info
 :<- [:final-answer]
 :<- [:game-over?]
 :<- [:current-attempt]
 :<- [:game-mode]
 :<- [:max-attempts]
 (fn [[final-answer game-over? current-attempt game-mode max-attempts] _]
   (let [{:keys [game-over? success?]} game-over?]
     (if game-over?
       {:game-over? true
        :messages [(if success?
                     (attempt-number->message current-attempt)
                     (str "Resposta: " final-answer))]}
       (let [attempts-left (- max-attempts current-attempt)
             suffix (when (> attempts-left 1) "s")]
         {:game-over? false
          :messages [(str attempts-left " tentativa" suffix " restante" suffix)
                     (case game-mode
                       :bento "com palavras em geral"
                       :capitu "com palavras de “Dom Casmurro”"
                       nil)]})))))

(rf/reg-sub
 :stats-info
 :<- [:stats]
 :<- [:game-mode]
 :<- [:max-attempts]
 (fn [[stats game-mode max-attempts] _]
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
 :<- [:used-letters]
 :<- [:game-over?]
 (fn [[used-letters game-over?] _]
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
