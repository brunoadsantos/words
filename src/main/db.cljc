(ns main.db
  (:require [main.word-bank :as wb]))

(defn ^:private build-word-map [words]
  (->> words
       (map (fn [word-list]
              (if (= 1 (count word-list))
                (repeat 2 (first word-list))
                word-list)))
       (map (juxt first rest))
       (into {})))

(def ^:private bento-words
  (delay (let [word-map (build-word-map wb/bento-words)]
           {:word-map word-map
            :valid-attempts (-> word-map keys set)
            :answers wb/common-words})))

(def ^:private capitu-words
  (delay (let [word-map (build-word-map wb/capitu-words)
               answers (keys word-map)]
           {:word-map word-map
            :valid-attempts (set answers)
            :answers answers})))

(defn ^:private get-words [game-mode]
  (case game-mode
    :bento @bento-words
    :capitu @capitu-words))

(defn new-game [{:keys [game-mode game-state]}]
  (let [{:keys [word-map valid-attempts answers]} (get-words game-mode)]
    (merge {:valid-attempts valid-attempts
            :word-map word-map
            :game-mode game-mode}
           (if game-state
             game-state
             (let [answer (rand-nth answers)]
               #?(:cljs (js/console.log answer))
               {:answer answer
                :wrong-letters #{}
                :correct-letters #{}
                :misplaced-letters #{}
                :attempts {}
                :attempt-number 0
                :max-attempts 6})))))
