(ns main.playground
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.text Normalizer Normalizer$Form]))

(defn remove-punctuation [s]
  (when s (str/replace s #"[.,;!?:\"\'0-9()]" "")))

(defn sanitize-word [w]
  (-> w
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"[^a-zA-Z]" "")))

(defn valid-word? [w]
  (and (= (-> w count) 5)
       (not (str/includes? w "-"))
       (not (str/includes? w "III"))
       (not (str/includes? w "DLX"))
       (not (str/includes? w "CXL"))
       (not (str/includes? w "CLX"))
       (not (str/includes? w "VII"))
       (not (str/includes? w "XX"))
       (not (str/includes? w "MM"))
       (not (str/includes? w "DD"))))

(defn process-words [in]
  (with-open [f (io/reader in)]
    (loop [line (remove-punctuation (.readLine f))
           word-map {}]
      (if line
        (let [words (-> line str/upper-case (str/split #"\s+"))
              valid-words (filter valid-word? words)]
          (recur (remove-punctuation (.readLine f))
                 (reduce (fn [wm w]
                           (let [s (sanitize-word w)]
                             (update wm s #(if % (conj % w) #{w}))))
                         word-map
                         valid-words)))
        word-map))))

(comment

  (def words
    (process-words (io/resource "data/aspell_PT_BR.txt")))

  (def dom-casmurro
    (process-words (io/resource "data/domCasmurro.txt")))

  (->> dom-casmurro keys count)
  (->> words keys count)

  (->> (set/intersection
        (set (keys words))
        (set (keys dom-casmurro)))
       sort
       vec
       (spit "common_words.cljs"))

  (->> words
       (map #(if (get (second %) (first %))
               [(first %)]
               (vec (concat [(first %)] (second %)))))
       (sort-by first)
       vec
       (spit "all_words.cljs"))
  )
