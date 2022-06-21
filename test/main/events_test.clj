(ns main.events-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
            [main.events]
            [main.subs]
            [matcher-combinators.standalone :refer [match?]]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]))

(defn ^:private fixture-re-frame
  [f]
  (let [restore-re-frame (atom nil)]
    (reset! restore-re-frame (rf/make-restore-fn))
    (f)
    (@restore-re-frame)))

(use-fixtures :each fixture-re-frame)

(defn ^:private stub-cofxs
  ([] (stub-cofxs {}))
  ([saved-game-state]
   (rf/reg-cofx
    :game-mode-from-url
    (fn game-mode [cofx]
      (assoc cofx :game-mode-from-url :bento)))

   (rf/reg-cofx
    :saved-game-state
    (fn game-state [cofx]
      (assoc cofx :saved-game-state saved-game-state)))))

(defn ^:private input-keys! [keys-seq]
  (run!
   #(rf/dispatch [:key-input (cond-> % (char? %) str)])
   keys-seq))

(defn ^:private check-new-game! [expected-game-mode]
  (let [game-mode (rf/subscribe [:game-mode])
        answer (rf/subscribe [:answer])
        current-attempt (rf/subscribe [:current-attempt])]
    (is (= expected-game-mode @game-mode))
    (is (string? @answer))
    (is (= (case expected-game-mode :bento 5 :capitu 6) (-> @answer count)))
    (is (= 0 @current-attempt))))

(defn ^:private submit-attempt! []
  (let [answer (rf/subscribe [:answer])
        revealing? (rf/subscribe [:revealing?])]
    (input-keys! [:check])
    (is @revealing?)
    (rf/dispatch [:letter-revealed (-> @answer count dec)])
    (is (not @revealing?))))

(defn ^:private check-game-over! [{:keys [game-over? success?]}]
  (let [game-over-info (rf/subscribe [:game-over-info])]
    (is (match? {:game-over? game-over? :success? success?} @game-over-info))))

(defn ^:private check-stats! [expected-stats]
  (let [stats-info (rf/subscribe [:stats-info])]
    (is (match? expected-stats @stats-info))))

(deftest full-games-test
  (rf-test/run-test-sync
   (stub-cofxs)
   (let [answer (rf/subscribe [:answer])
         current-attempt (rf/subscribe [:current-attempt])
         attempt-0 (rf/subscribe [:display-attempt-n 0])
         attempt-row-0 (rf/subscribe [:attempt-row-n 0])
         attempt-row-1 (rf/subscribe [:attempt-row-n 1])]
     (testing "Starting new game in default (bento) mode"
       (rf/dispatch [:new-game])
       (check-new-game! :bento))

     (is (match? [{:current-row? true
                   :current? true
                   :idx 0
                   :last-added? false
                   :letter ""
                   :result :unchecked}
                  {:current-row? true
                   :current? false
                   :idx 1
                   :last-added? false
                   :letter ""
                   :result :unchecked}
                  {:current-row? true
                   :current? false
                   :idx 2
                   :last-added? false
                   :letter ""
                   :result :unchecked}
                  {:current-row? true
                   :current? false
                   :idx 3
                   :last-added? false
                   :letter ""
                   :result :unchecked}
                  {:current-row? true
                   :current? false
                   :idx 4
                   :last-added? false
                   :letter ""
                   :result :unchecked}]
                 @attempt-row-0))

     (testing "Input single letter to attempt 0"
       (input-keys! "A")
       (is (= ["A" "" "" "" ""] @attempt-0))
       (is (match? [{:current-row? true
                     :current? false
                     :idx 0
                     :last-added? true
                     :letter "A"
                     :result :unchecked}
                    {:current-row? true
                     :current? true
                     :idx 1
                     :last-added? false
                     :letter ""
                     :result :unchecked}]
                   (-> @attempt-row-0 vec (subvec 0 2)))))

     (testing "Submitting incomplete attempt is rejected"
       (input-keys! [:check])
       (is (= 0 @current-attempt)))

     (testing "Delete last letter"
       (input-keys! [:delete])
       (is (= ["" "" "" "" ""] @attempt-0))
       (is (match? [{:current-row? true
                     :current? true
                     :idx 0
                     :last-added? false
                     :letter ""
                     :result :unchecked}
                    {:current-row? true
                     :current? false
                     :idx 1
                     :last-added? false
                     :letter ""
                     :result :unchecked}]
                   (-> @attempt-row-0 vec (subvec 0 2)))))

     (testing "Input and submit valid attempt different from answer"
       (let [last-letter (if (= "ROSEA" @answer) "O" "A")]
         (input-keys! "ROSE")
         (input-keys! last-letter)
         (submit-attempt!)
         (is (= ["R" "Ó" "S" "E" last-letter] @attempt-0))
         (is (match? [{:current-row? false
                       :current? false
                       :last-added? false
                       :letter "R"}
                      {:current-row? false
                       :current? false
                       :last-added? false
                       :letter "Ó"}
                      {:current-row? false
                       :current? false
                       :last-added? false
                       :letter "S"}
                      {:current-row? false
                       :current? false
                       :last-added? false
                       :letter "E"}
                      {:current-row? false
                       :current? false
                       :last-added? false
                       :letter last-letter}]
                     @attempt-row-0))))

     (testing "Current attempt is increased but game is not over"
       (is (= 1 @current-attempt))
       (check-game-over! {:game-over? false :success? false}))

     (testing "Input and submit correct answer"
       (input-keys! @answer)
       (submit-attempt!)
       (is (match? (repeat 5 {:result :correct
                              :current-row? true})
                   @attempt-row-1))
       (is (= 1 @current-attempt))
       (check-game-over! {:game-over? true :success? true})
       (check-stats! {:total-games-played 1
                      :total-wins 1
                      :attempts-distribution [{:attempt-number 2
                                               :number-of-wins 1
                                               :fraction 1}]}))

     (testing "Re-stub coeffects to simulate persisted state and start new game"
       (stub-cofxs {:bento @rf-db/app-db})
       (rf/dispatch [:new-game {:game-mode :bento :force-new? true}])
       (check-new-game! :bento))

     (testing "Use all attempts with wrong word to end game"
       (run! (fn [_]
               (input-keys! (if (= @answer "BENTO") "CAMPO" "BENTO"))
               (submit-attempt!))
             (range 6))
       (check-game-over! {:game-over? true :success? false}))

     (testing "Stats should have accumulated from previous game in same mode"
       (check-stats! {:total-games-played 2
                      :total-wins 1
                      :attempts-distribution [{:attempt-number 2
                                               :number-of-wins 1
                                               :fraction 1}]}))

     (testing "Full game run on capitu mode"
       (rf/dispatch [:switch-game-mode])
       (check-new-game! :capitu)

       (run! (fn [_]
               (input-keys! (if (= @answer "CAPITU") "PALIDO" "CAPITU"))
               (submit-attempt!))
             (range 6))

       (is (= 5 @current-attempt))
       (check-game-over! {:game-over? true :success? false})
       (check-stats! {:total-games-played 1
                      :total-wins 0
                      :attempts-distribution empty?})))))
