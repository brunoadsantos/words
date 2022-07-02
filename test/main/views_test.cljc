(ns main.views-test
  (:require #?@(:clj  [[clojure.test :refer [deftest is]]]
                :cljs [[cljs.test :refer [deftest is]]])
            [main.logic :as l]
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
  (is (match? [:div {:class "letter pulse correct"} "A"]
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
  (is (match? [:div.attempt-rows.centered-div [[(m/equals v/attempt-row) {:key 0 :attempt-number 0}]]]
              (v/attempt-rows {:max-attempts 1})))
  (is (match? [:div.attempt-rows.centered-div (repeat 6 [(m/equals v/attempt-row) {:key integer? :attempt-number integer?}])]
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
                         :status :correct})))
  (is (match? [:button.keyboard-btn.control {:on-click fn?}
               [:span [(m/equals v/icon) :some-icon]]]
              (v/button {:icon-name :some-icon
                         :code :some-code}))))

(defn ^:private letters->row-props [letters used-letters]
  {:key "row"
   :row (map (fn [letter] (l/letter->button-props (str letter) used-letters)) letters)})

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
              (v/button-row (letters->row-props "ABC" {}))))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :wrong}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row (letters->row-props "ABC" {:wrong-letters #{"A"}}))))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :misplaced}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row (letters->row-props "ABC" {:wrong-letters #{"A"}
                                                       :misplaced-letters #{"A"}}))))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :correct}]
                [(m/equals v/button) {:text "B"
                                      :status nil}]
                [(m/equals v/button) {:text "C"
                                      :status nil}]]]
              (v/button-row (letters->row-props "ABC" {:wrong-letters #{"A"}
                                                       :misplaced-letters #{"A"}
                                                       :correct-letters #{"A"}}))))
  (is (match? [:div.centered-div.button-row
               [[(m/equals v/button) {:text "A"
                                      :status :wrong}]
                [(m/equals v/button) {:text "B"
                                      :status :misplaced}]
                [(m/equals v/button) {:text "C"
                                      :status :correct}]]]
              (v/button-row (letters->row-props "ABC" {:wrong-letters #{"A"}
                                                       :misplaced-letters #{"B"}
                                                       :correct-letters #{"C"}})))))

(deftest keyboard
  (is (match? [:div.keyboard (repeat 5 [(m/equals v/button-row) {:some-prop 1}])]
              (v/keyboard {:rows (repeat 5 {:some-prop 1})}))))

(deftest title
  (is (match? [:div.title.centered-div [:h1 [:span {} "BENTO" [:sub [(m/equals v/icon) :swap_horiz]]]]]
              (v/title {:game-mode :bento})))
  (is (match? [:div.title.centered-div [:h1 [:span {} "CAPITU" [:sub [(m/equals v/icon) :swap_horiz]]]]]
              (v/title {:game-mode :capitu}))))

(deftest game-over-alert
  (is (match? [:div.centered-div.game-over [:<> [:h3 "m1"] [:h4 "m2"]]]
              (v/game-over-alert {:messages ["m1" "m2"]})))
  (is (match? [:div.centered-div.game-over [:h3.game-over-banner.pulse "some message"]]
              (v/game-over-alert {:game-over? true
                                  :messages ["some message"]}))))
