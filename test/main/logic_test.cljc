(ns main.logic-test
  (:require #?@(:clj  [[clojure.test :refer [deftest is]]]
                :cljs [[cljs.test :refer [deftest is]]])
            [main.logic :as l]
            [matcher-combinators.standalone :refer [match?]]))

(deftest get-word
  (let [words #{"ABC" "DEF"}
        db {:word-map {"ABC" (seq words)}}]
    (is (words (l/get-word db "ABC")))))

(defn with-attempt [{:keys [attempt-number] :as db} attempt-str]
  (assoc-in db [:attempts attempt-number :attempt] (map str attempt-str)))

(deftest get-letter-results
  (let [db {:answer "FALAS"
            :attempt-number 0}]
    (is (= (repeat 5 :correct)
           (l/get-letter-results (with-attempt db "FALAS"))))
    (is (= (repeat 5 :wrong)
           (l/get-letter-results (with-attempt db "MINTO"))))
    (is (= (repeat 5 :misplaced)
           (l/get-letter-results (with-attempt db "ASFLA"))))
    (is (= [:correct :correct :correct :correct :wrong]
           (l/get-letter-results (with-attempt db "FALAR"))))
    (is (= [:wrong :correct :correct :correct :correct]
           (l/get-letter-results (with-attempt db "SALAS"))))
    (is (= [:misplaced :correct :correct :correct :wrong]
           (l/get-letter-results (with-attempt db "SALAO"))))
    (is (= [:misplaced :wrong :misplaced :wrong :wrong]
           (l/get-letter-results (with-attempt db "ATADO"))))
    (is (= [:misplaced :correct :wrong :wrong :wrong]
           (l/get-letter-results (with-attempt db "AABBA"))))))

(defn ^:private get-used-letters [db attempt-str]
  (let [db (with-attempt db attempt-str)
        letter-results (l/get-letter-results db)]
    (l/get-used-letters-from-current-attempt db letter-results)))

(deftest get-used-letters-from-current-attempt
  (let [db {:answer "FALAS"
            :attempt-number 0}]
    (let [{:keys [correct-letters]} (get-used-letters db "FALAS")]
      (is (= #{"F" "A" "L" "S"} correct-letters)))
    (let [{:keys [wrong-letters]} (get-used-letters db "MINTO")]
      (is (= #{"M" "I" "N" "T" "O"} wrong-letters)))
    (let [{:keys [misplaced-letters]} (get-used-letters db "ASFLA")]
      (is (= #{"F" "A" "L" "S"} misplaced-letters)))
    (let [{:keys [correct-letters wrong-letters]}
          (get-used-letters db "SALAS")]
      (is (= #{"S" "A" "L"} correct-letters))
      (is (= #{"S"} wrong-letters)))
    (let [{:keys [correct-letters misplaced-letters wrong-letters]}
          (get-used-letters db "AABBA")]
      (is (= #{"A"} correct-letters))
      (is (= #{"A"} misplaced-letters))
      (is (= #{"A" "B"} wrong-letters)))
    (let [{:keys [correct-letters misplaced-letters wrong-letters]}
          (get-used-letters db "SALAO")]
      (is (= #{"A" "L"} correct-letters))
      (is (= #{"S"} misplaced-letters))
      (is (= #{"O"} wrong-letters)))
    (let [{:keys [correct-letters misplaced-letters wrong-letters]}
          (-> db
              (get-used-letters "SALAO")
              (get-used-letters "FALAS"))]
      (is (= #{"F" "A" "L" "S"} correct-letters))
      (is (= #{"S"} misplaced-letters))
      (is (= #{"O"} wrong-letters)))))

(deftest get-hint
  (let [db {:answer "FACIL"
            :hints-points 1}]
    (is (match? {:hint-letters not-empty
                 :hints-points 0}
                (l/get-hint db))))
  (let [db {:answer "FACIL"
            :hints-points 5}]
    (is (match? {:hint-letters #{"F" "A" "C" "I" "L"}
                 :misplaced-letters #{"F" "A" "C" "I" "L"}
                 :hints-points 0}
                (-> db
                    (l/get-hint)
                    (l/get-hint)
                    (l/get-hint)
                    (l/get-hint)
                    (l/get-hint)))))
  (let [db {:hints-points 0}]
    (is (match? nil (l/get-hint db))))
  (let [db {:answer "FACIL"
            :correct-letters #{"F" "A" "C" "I" "L"}
            :hints-points 1}]
    (is (match? nil (l/get-hint db))))
  (let [db {:answer "SALAS"
            :correct-letters #{"S" "A" "L"}
            :hints-points 1}]
    (is (match? nil (l/get-hint db))))
  (let [db {:answer "FACIL"
            :correct-letters #{"F" "A"}
            :misplaced-letters #{"C" "I"}
            :hints-points 2}]
    (is (match? {:hint-letters #{"L"}
                 :misplaced-letters #{"C" "I" "L"}
                 :hints-points 1}
                (l/get-hint db)))))
