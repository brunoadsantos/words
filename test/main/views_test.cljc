(ns main.views-test
  (:require #?@(:clj  [[clojure.test :refer [deftest is]]]
                :cljs [[cljs.test :refer [deftest is]]])
            [main.views :as v]
            [matcher-combinators.matchers :as m]
            [matcher-combinators.standalone :refer [match?]]))

(deftest letter-slot
  (is (match? [:div {:key "0"
                     :class "letter correct"
                     :data-letter-idx 0}
               "A"]
              (v/letter-slot {:result :correct
                              :idx 0
                              :letter "A"})))
  (is (match? [:div {:class "letter last-added-letter correct"} "A"]
              (v/letter-slot {:result :correct
                              :idx 0
                              :last-added? true
                              :letter "A"})))
  (is (match? [:div {:class "letter current-row current-letter correct"} "A"]
              (v/letter-slot {:result :correct
                              :idx 0
                              :current-row? true
                              :current? true
                              :letter "A"})))
  (is (match? [:div {:class "letter current-row correct"} "A"]
              (v/letter-slot {:result :correct
                              :idx 0
                              :current-row? true
                              :letter "A"}))))

(deftest icon
  (is (= [:span.material-symbols-outlined "abc"] (v/icon :abc))))

(deftest attempt-rows
  (is (match? [:div.attempt-rows [[(m/equals v/attempt-row) {:key 0 :attempt-number 0}]]]
              (v/attempt-rows {:max-attempts 1})))
  (is (match? [:div.attempt-rows (repeat 6 [(m/equals v/attempt-row) {:key integer? :attempt-number integer?}])]
              (v/attempt-rows {:max-attempts 6}))))

(deftest button
  (is (match? [:button.keyboard-btn {:on-click fn?}
               [:span {:class nil} "A"]]
              (v/button {:text "A" :code :a})))
  (is (match? [:button.keyboard-btn {:on-click fn?}
               [:span {:class "wrong"} "A"]]
              (v/button {:text "A" :code :a
                         :status :wrong})))
  (is (match? [:button.keyboard-btn {:on-click fn?}
               [:span {:class "misplaced"} "A"]]
              (v/button {:text "A" :code :a
                         :status :misplaced})))
  (is (match? [:button.keyboard-btn {:on-click fn?}
               [:span {:class "correct"} "A"]]
              (v/button {:text "A" :code :a
                         :status :correct}))))

(deftest button-row
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:key "A"
                                      :text "A"
                                      :code "A"
                                      :status nil}]
                [(m/equals v/button) {:key "B"
                                      :text "B"
                                      :code "B"
                                      :status nil}]
                [(m/equals v/button) {:key "C"
                                      :text "C"
                                      :code "C"
                                      :status nil}]]]
              (v/button-row "ABC" {})))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :wrong}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row "ABC" {:wrong-letters #{"A"}})))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :misplaced}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row "ABC" {:wrong-letters #{"A"}
                                   :misplaced-letters #{"A"}})))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :correct}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row "ABC" {:wrong-letters #{"A"}
                                   :misplaced-letters #{"A"}
                                   :correct-letters #{"A"}})))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :wrong}]
                [(m/equals v/button) {:text "B"
                                      :status :misplaced}]
                [(m/equals v/button) {:text "C"
                                      :status :correct}]]]
              (v/button-row "ABC" {:wrong-letters #{"A"}
                                   :misplaced-letters #{"B"}
                                   :correct-letters #{"C"}}))))

(deftest title
  (is (match? [:div.title [:h1 [:span {} "BENTO" [:sub [(m/equals v/icon) :sync]]]]]
              (v/title {:game-mode :bento})))
  (is (match? [:div.title [:h1 [:span {} "CAPITU" [:sub [(m/equals v/icon) :sync]]]]]
              (v/title {:game-mode :capitu}))))

(deftest game-over-alert
  (is (match? [:div.centered-div.game-over {:style {:visibility "hidden"}} vector?]
              (v/game-over-alert {})))
  (is (match? [:div.centered-div.game-over {:style {:visibility "visible"}} vector?]
              (v/game-over-alert {:game-over? true})))
  (is (match? [:div.centered-div.game-over {} [:span.game-over-banner "Resposta: FINAL"]]
              (v/game-over-alert {:game-over? true
                                  :final-answer "FINAL"})))
  (is (match? [:div.centered-div.game-over {} [:span.game-over-banner "Ufa!"]]
              (v/game-over-alert {:game-over? true
                                  :success? true})))
  (is (match? [:div.centered-div.game-over {} [:span.game-over-banner "Muito bem!"]]
              (v/game-over-alert {:game-over? true
                                  :success? true
                                  :victory-attempt-number 4}))))
