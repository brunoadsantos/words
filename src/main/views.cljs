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
      (if game-over?
        [:button {:on-click #(rf/dispatch [:new-game {:force-new? true}])}
         [:span.control
          [icon :refresh]]]
        [:<>
         [:button {:on-click #(rf/dispatch [:key-input :delete])
                   :disabled revealing?}
          [:span.control
           [icon :backspace]]]
         [:button {:on-click #(rf/dispatch [:key-input :check])
                   :disabled revealing?}
          [:span.control
           [icon :done]]]])]]))

(defn title []
  (let [revealing? @(rf/subscribe [:revealing?])
        game-mode @(rf/subscribe [:game-mode])
        new-game-mode (if (= :bento game-mode) :capitu :bento)]
    [:h1
     [:span {:on-click #(when-not revealing?
                          (rf/dispatch [:new-game {:game-mode new-game-mode}]))}
      (if (= :bento game-mode) "BENTO" "CAPITU")
      [:sub [icon :sync]]]]))

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

(defn indicator-line [line]
  [:div {:style {:min-width "60%"
                 :text-align :left
                 :margin-left "8pt"}}
   line])

(defn overlay [id content]
  (let [shown? @(rf/subscribe [:overlay-shown? id])]
    [:div.overlay {:style {:left (if shown? "0%" "-110%")}}
     [:div.overlay-content
      content]
     [:button.close-btn {:on-click #(rf/dispatch [:set-overlay-shown id false])}
      [:div [icon "close"]]]]))

(defn about []
  [:<>
   [:h1 "Bento & Capitu"]
   [:p "Você tem 6 tentativas para adivinhar uma palavra aleatória retirada do livro " [:em "Dom Casmurro"]
    ", de Machado de Assis. Cada tentativa indicará:"]
   [:div.centered-div
    [:div.letter.correct "A"]
    [indicator-line "Letra na posição correta"]]
   [:div.centered-div
    [:div.letter.misplaced "A"]
    [indicator-line "Letra na posição incorreta"]]
   [:div.centered-div
    [:div.letter.wrong "A"]
    [indicator-line "Letra não faz parte da palavra"]]
   [:p "No modo " [:em "Bento"] ", as palavras têm 5 letras e as tentativas são palavras em geral."]
   [:p "No modo " [:em "Capitu"] ", as palavras têm 6 letras e as tentativas são restritas às palavras que aparecem no livro " [:em "Dom Casmurro"] "."]
   [:p "Toque ou clique no título da página para alternar entre os modos. O progresso em cada um deles é mantido."]
   [:p "O jogo funciona offline e pode ser instalado como um app pelo menu do navegador."]
   [:p [:a {:href "https://github.com/brunoadsantos/words"} "Código no GitHub"]]
   [:p [:a {:href "https://machado.mec.gov.br/obra-completa-lista/itemlist/category/23-romance"}
        "Obra de Machado de Assis em domínio público"]]])

(defn bar [{:keys [idx amount max-amount]}]
  (let [max-size 160
        bar-size (* max-size (/ amount max-amount))]
    [:div.bar {:key (str "bar" idx)
               :style {:width (str max-size "pt")}}
     [:div {:style {:width (str bar-size "pt")}}
      amount]]))

(defn attempts-distribution [stats max-attempts]
  (when (-> stats :total-wins pos-int?)
    [:<>
     [:h3 "Distribuição por número de tentativas"]
     (let [max-amount (->> stats :attempts-to-win vals (apply max))]
       (for [i (range max-attempts)
             :let [i (inc i)
                   amount (get-in stats [:attempts-to-win i] 0)]
             :when (pos-int? amount)]
         [:div.centered-div {:key (str i)}
          (str i)
          [bar {:amount amount
                :max-amount max-amount}]]))]))

(defn stats []
  (let [game-mode @(rf/subscribe [:game-mode])
        max-attempts @(rf/subscribe [:max-attempts])
        stats @(rf/subscribe [:stats])]
    [:<>
     [:h1 (if (= game-mode :bento) "BENTO" "CAPITU")]
     [:h3 "Estatísticas"]
     [:p "Jogos finalizados: " (or (:total-games-played stats) 0)]
     [:p "Vitórias: " (or (:total-wins stats) 0)]
     [attempts-distribution stats max-attempts]]))

(defn overlay-trigger [id icon-name styling]
  [:button {:style (merge {:position :absolute
                           :top "8pt"}
                          styling)
            :on-click #(rf/dispatch [:set-overlay-shown id true])}
   [:div [icon icon-name]]])

(defn body []
  [:div
   [overlay :about [about]]
   [overlay :stats [stats]]
   [overlay-trigger :about :info {:left "8pt"}]
   [overlay-trigger :stats :leaderboard {:right "8pt"}]
   [title]
   [attempt-rows]
   [game-over-alert]
   [keyboard]])

(defn app []
  [body])
