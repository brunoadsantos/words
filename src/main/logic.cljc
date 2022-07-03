(ns main.logic
  (:require [clojure.string :as str]))

(defn get-word [{:keys [word-map]} w]
  (-> word-map (get w) rand-nth))

(defn push-to-current-attempt [db input]
  (let [attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])
        attempt-size (count attempt)
        answer (:answer db)
        answer-size (count answer)]
    (if (< attempt-size answer-size)
      (-> db
          (update-in [:attempts attempt-number :attempt] (fnil conj []) (str/upper-case input))
          (assoc :last-letter-position-added [attempt-number attempt-size]))
      db)))

(defn pop-from-current-attempt [db]
  (let [attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])
        attempt-size (count attempt)]
    (if (< 0 attempt-size)
      (-> db
          (update-in [:attempts attempt-number :attempt] pop)
          (dissoc :last-letter-position-added))
      db)))

(defn valid-current-attempt? [db]
  (let [valid-attempts (:valid-attempts db)
        attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])
        attempt-size (count attempt)
        answer (:answer db)
        answer-size (count answer)]
    (and (= attempt-size answer-size) (get valid-attempts (str/join attempt)))))

(defn new-attempt [db]
  (let [attempt-number (-> db :attempt-number)]
    (-> db
        (assoc :attempt-number (inc attempt-number)
               :revealing? false))))

(defn add-valid-attempt [db]
  (let [attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])]
    (-> db
        (assoc-in [:attempts attempt-number :valid-attempt] (get-word db (str/join attempt))))))

(defn check-current-attempt [db letter-results]
  (let [attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])
        answer (:answer db)]
    (-> db
        (assoc :success? (= (str/join attempt) (str/join answer)))
        (assoc-in [:attempts attempt-number :checked?] true)
        (assoc-in [:attempts attempt-number :letter-results] letter-results))))

(defn check-game-over [db]
  (let [answer (:answer db)
        attempt-number (-> db :attempt-number)
        max-attempts (-> db :max-attempts)
        success? (-> db :success?)
        game-over? (or success? (= max-attempts attempt-number))]
    (-> db
        (assoc :game-over? game-over?)
        (cond-> game-over?
          (-> (update :attempt-number dec)
              (update-in [:stats :total-games-played] (fnil inc 0))
              (cond-> success? (update-in [:stats :attempts-to-win attempt-number] (fnil inc 0))
                      success? (update-in [:stats :total-wins] (fnil inc 0))
                      success? (update :hints-points (fnil inc 0))
                      (not success?) (assoc :final-answer (get-word db answer))))))))

(def ^:private letter-result->db-kw
  {:correct :correct-letters
   :misplaced :misplaced-letters
   :wrong :wrong-letters
   :hint :hint-letters})

(defn ^:private collect-used-letter
  [db [letter result]]
  (update db
          (letter-result->db-kw result)
          (fnil conj #{})
          letter))

(defn get-used-letters-from-current-attempt [db letter-results]
  (let [attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])]
    (->> (map vector attempt letter-results)
         (reduce collect-used-letter db))))

(defn get-letter-results [db]
  (let [answer-seq (->> (:answer db) #?(:clj (map str)))
        answer-letter-count (frequencies answer-seq)
        attempt-number (-> db :attempt-number)
        attempt (get-in db [:attempts attempt-number :attempt])
        correct-letter-count (->> (map (fn [l1 l2] (when (= l1 l2) l1)) attempt answer-seq)
                                  (filter some?)
                                  (frequencies))
        remaining-letter-count (->> answer-letter-count
                                    (map (fn [[k v]]
                                           [k (- v (get correct-letter-count k 0))]))
                                    (into {}))]
    (loop [results []
           letter-pairs (map vector attempt answer-seq)
           remaining remaining-letter-count]
      (if-let [[l1 l2] (first letter-pairs)]
        (let [result (cond
                       (= l1 l2) :correct
                       (pos-int? (get remaining l1)) :misplaced
                       :else :wrong)]
          (recur (conj results result)
                 (rest letter-pairs)
                 (cond-> remaining
                   (= result :misplaced) (update l1 dec))))
        results))))

(defn ^:private letter-status [letter {:keys [correct-letters wrong-letters misplaced-letters]}]
  (condp get letter
    correct-letters :correct
    misplaced-letters :misplaced
    wrong-letters :wrong
    nil))

(defn letter->button-props [l used-letters]
  {:text l
   :code l
   :key l
   :status (letter-status l used-letters)})

(defn get-hint [db]
  (when (-> db (:hints-points 0) pos?)
    (let [answer-seq (->> (:answer db) #?(:clj (map str)))
          correct-letters (-> db :correct-letters set)
          misplaced-letters (-> db :misplaced-letters set)
          unknown-letters (->> answer-seq
                               (remove correct-letters)
                               (remove misplaced-letters))
          hint (when (seq unknown-letters) (rand-nth unknown-letters))]
      (when hint
        (-> db
            (update :hints-points dec)
            (collect-used-letter [hint :misplaced])
            (collect-used-letter [hint :hint]))))))
