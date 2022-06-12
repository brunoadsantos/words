(ns main.views
  (:require [re-frame.core :as rf]))

(defn letter-slot [{:keys [letter result idx current-row? current? last-added?]}]
  [:div
   {:key (str idx)
    :class (str "letter "
                (when current-row? "current-row ")
                (when (and current-row? current?) "current-letter ")
                (when last-added? "last-added-letter ")
                (name result))}
   letter])

(defn attempt-row [{:keys [attempt-number]}]
  (let [{:keys [attempt valid-attempt]} @(rf/subscribe [:attempt attempt-number])
        letter-results @(rf/subscribe [:letter-results attempt-number])
        current-attempt @(rf/subscribe [:current-attempt])
        last-letter-position-added @(rf/subscribe [:last-letter-position-added])
        id (str "row" attempt-number)]
    [:div.centered-div {:key id :id id}
     (->> (map vector (or valid-attempt attempt) letter-results)
          (map-indexed (fn [idx [letter result]]
                         {:letter letter
                          :result result
                          :active? (<= attempt-number current-attempt)
                          :current? (= idx (->> attempt (remove empty?) count))
                          :current-row? (= attempt-number current-attempt)
                          :last-added? (= last-letter-position-added [attempt-number idx])
                          :idx idx}))
          (map letter-slot))]))

(defn button [{:keys [text-or-code correct? wrong? misplaced?]}]
  (let [code (if (string? text-or-code) text-or-code (:code text-or-code))
        text (if (string? text-or-code) text-or-code (:text text-or-code))]
    [:button {:key code
              :on-click #(rf/dispatch [:key-input code])}
     [:span {:class (cond
                      correct? "correct"
                      wrong? "wrong"
                      misplaced? "misplaced"
                      :else "")}
      (str text)]]))

(defn button-row [row {:keys [correct-letters wrong-letters misplaced-letters]}]
  [:div.centered-div
   (->> row
        (map (fn [text-or-code]
               {:text-or-code text-or-code
                :correct? (get correct-letters text-or-code)
                :wrong? (get wrong-letters text-or-code)
                :misplaced? (get misplaced-letters text-or-code)}))
        (map button))])

(defn icon [icon-name]
  [:span.material-symbols-outlined
   (name icon-name)])

(defn keyboard []
  (let [used-letters @(rf/subscribe [:used-letters])
        revealing? @(rf/subscribe [:revealing?])
        {:keys [game-over?]} @(rf/subscribe [:game-over?])]
    [:div
     [button-row ["Q" "W" "E" "R" "T" "Y" "U" "I" "O" "P"] used-letters]
     [button-row ["A" "S" "D" "F" "G" "H" "J" "K" "L"] used-letters]
     [button-row ["Z" "X" "C" "V" "B" "N" "M"] used-letters]

     [:div.centered-div
      [:button {:on-click #(rf/dispatch [:key-input :delete])
                :disabled revealing?}
       [:span.control
        [icon :backspace]]]
      [:button {:on-click #(rf/dispatch [:key-input :check])
                :disabled revealing?}
       [:span.control
        [icon :done]]]

      (when game-over?
        [:button {:on-click #(rf/dispatch [:new-game {:force-new? true}])}
         [:span.control
          [icon :refresh]]])]]))

(defn title []
  (let [revealing? @(rf/subscribe [:revealing?])
        game-mode @(rf/subscribe [:game-mode])
        new-game-mode (if (= :bento game-mode) :capitu :bento)]
    [:h1 {:on-click #(when-not revealing?
                       (rf/dispatch [:new-game {:game-mode new-game-mode}]))}
     (if (= :bento game-mode) "BENTO" "CAPITU")
     [:sub [icon :sync]]]))

(defn attempt-rows []
  [:div
   (for [i (range @(rf/subscribe [:max-attempts]))]
     [attempt-row {:key i
                   :attempt-number i}])])

(defn game-over-alert []
  (let [{:keys [final-answer]} @(rf/subscribe [:answer])
        current-attempt @(rf/subscribe [:current-attempt])
        {:keys [game-over? success?]} @(rf/subscribe [:game-over?])]
    [:div.centered-div
     {:style {:visibility (if game-over? "visible" "hidden")
              :margin "8pt"}}
     [:span.game-over-banner
      (if success?
        (case current-attempt
          0 "Ótimo chute! ;)"
          1 "Excelente!"
          2 "Impressionante!"
          3 "Ótimo!"
          4 "Muito bem!"
          "Ufa!")
        (str "Resposta: " final-answer))]]))

(defn body []
  [:div
   [title]
   [attempt-rows]
   [game-over-alert]
   [keyboard]])

(defn app []
  [body])
