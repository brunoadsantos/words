(ns main.render-test
  (:require ["@testing-library/react" :as rtl]
            [cljs.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [day8.re-frame.test :as rf-test]
            [main.core]
            [main.effects]
            [main.events]
            [main.subs]
            [main.views :as v]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [reagent.core :as r]))

(def fixture-re-frame
  (let [restore-re-frame (atom nil)]
    {:before #(do (reset! restore-re-frame (rf/make-restore-fn))
                  (reset! main.effects/local-storage {}))
     :after  #(@restore-re-frame)}))

(use-fixtures :each {:before (comp (:before fixture-re-frame))
                     :after (comp rtl/cleanup (:after fixture-re-frame))})

(defn with-mounted-component [component f]
  (let [mounted-component (rtl/render (r/as-element component))]
    (try
      (f mounted-component)
      (finally
        (.unmount mounted-component)
        (r/flush)))))

(defn query-selector [query]
  (-> js/document
      (.querySelector query)))

(defn get-attempt-n [n]
  (->> (-> (query-selector (str "#row" n))
           (.-childNodes))
       (map #(.-innerText %))
       (str/join)))

(defn dispatch-and-flush [event]
  (rf/dispatch event)
  (r/flush))

(defn title-inner-text []
  (-> "h1>span"
      (query-selector)
      (.-innerText)))

(defn input-attempt-and-check! [attempt]
  (run! #(dispatch-and-flush [:key-input (str %)]) attempt)
  (dispatch-and-flush [:key-input :check])
  (dispatch-and-flush [:letter-revealed (-> attempt count dec)]))

(deftest switch-game-mode
  (rf-test/run-test-sync
   (with-mounted-component
     [v/body]
     (fn [_component]
       (dispatch-and-flush [:new-game])
       (is (str/starts-with? (title-inner-text) "BENTO"))
       (is (= 5 (-> (query-selector "#row0") (.-childNodes) (.-length))))
       (dispatch-and-flush [:switch-game-mode])
       (is (str/starts-with? (title-inner-text) "CAPITU"))
       (is (= 6 (-> (query-selector "#row0") (.-childNodes) (.-length))))))))

(deftest bento-happy-case
  (rf-test/run-test-sync
   (with-mounted-component
     [v/body]
     (fn [component]
       (dispatch-and-flush [:new-game])
       (is (str/starts-with? (title-inner-text) "BENTO"))
       (let [answer (:answer @rf-db/app-db)
             wrong-answer (case answer "BENTO" "MUITO" "BENTO")]
         (input-attempt-and-check! wrong-answer)
         (is (= wrong-answer (get-attempt-n 0)))
         (input-attempt-and-check! answer))
       (is (-> component
               (.getByText "Excelente!")))))))

(deftest bento-game-over
  (rf-test/run-test-sync
   (with-mounted-component
     [v/body]
     (fn [component]
       (dispatch-and-flush [:new-game])
       (is (str/starts-with? (title-inner-text) "BENTO"))
       (let [answer (:answer @rf-db/app-db)
             max-attempts (:max-attempts @rf-db/app-db)
             wrong-answer (case answer "BENTO" "MUITO" "BENTO")]
         (run! (fn [_]
                 (input-attempt-and-check! wrong-answer))
               (range max-attempts))
         (is (-> component
                 (.getByText (str "Resposta: " (:final-answer @rf-db/app-db))))))))))

(deftest capitu-happy-case
  (rf-test/run-test-sync
   (with-mounted-component
     [v/body]
     (fn [component]
       (dispatch-and-flush [:new-game {:game-mode :capitu}])
       (is (str/starts-with? (title-inner-text) "CAPITU"))
       (let [answer (:answer @rf-db/app-db)]
         (input-attempt-and-check! answer))
       (is (-> component
               (.getByText "Ótimo chute!")))))))

(deftest capitu-game-over
  (rf-test/run-test-sync
   (with-mounted-component
     [v/body]
     (fn [component]
       (dispatch-and-flush [:new-game {:game-mode :capitu}])
       (is (str/starts-with? (title-inner-text) "CAPITU"))
       (let [answer (:answer @rf-db/app-db)
             max-attempts (:max-attempts @rf-db/app-db)
             wrong-answer (case answer "CAPITU" "CIGANA" "CAPITU")]
         (run! (fn [_]
                 (input-attempt-and-check! wrong-answer))
               (range max-attempts))
         (is (-> component
                 (.getByText (str "Resposta: " (:final-answer @rf-db/app-db))))))))))
