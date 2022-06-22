(ns main.views
  (:require [re-frame.core :as rf]))

(defn letter-slot [{:keys [letter result idx current-row? current? last-added?]}]
  [:div
   {:key (str idx)
    :data-letter-idx idx
    :class (str "letter "
                (when current-row? "current-row ")
                (when (and current-row? current?) "current-letter ")
                (when last-added? "last-added-letter ")
                (name result))}
   letter])

(defn attempt-row [{:keys [attempt-number]}]
  (let [row @(rf/subscribe [:attempt-row-n attempt-number])
        id (str "row" attempt-number)]
    [:div.centered-div {:key id :id id}
     (map letter-slot row)]))

(defn button [{:keys [code text status]}]
  [:button {:on-click #(rf/dispatch [:key-input code])}
   [:span {:class (some-> status name)}
    (str text)]])

(defn button-row [row {:keys [correct-letters wrong-letters misplaced-letters]}]
  [:div.centered-div
   (->> row
        (map (fn [text-or-code]
               (let [text (cond-> text-or-code
                            (map? text-or-code) :text
                            :always str)]
                 {:key text
                  :text text
                  :code (cond-> text-or-code
                          (map? text-or-code) :code
                          :always str)
                  :status (condp get text
                            correct-letters :correct
                            misplaced-letters :misplaced
                            wrong-letters :wrong
                            nil)})))
        (map (partial vector button)))])

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

(def game-mode->str
  {:bento "BENTO"
   :capitu "CAPITU"})

(defn title [{:keys [game-mode]}]
  [:h1
   [:span {:on-click #(rf/dispatch [:switch-game-mode])}
    (game-mode->str game-mode)
    [:sub [icon :sync]]]])

(defn title-container []
  (let [props {:game-mode @(rf/subscribe [:game-mode])}]
    [title props]))

(defn attempt-rows [{:keys [max-attempts]}]
  [:div
   (for [i (range max-attempts)]
     [attempt-row {:key i
                   :attempt-number i}])])

(defn attempt-rows-container []
  (let [props {:max-attempts @(rf/subscribe [:max-attempts])}]
    [attempt-rows props]))

(defn game-over-alert [{:keys [final-answer victory-attempt-number game-over? success?]}]
  [:div.centered-div
   {:style {:visibility (if game-over? "visible" "hidden")
            :margin "8pt"}}
   [:span.game-over-banner
    (if success?
      (case victory-attempt-number
        0 "Ótimo chute!"
        1 "Excelente!"
        2 "Impressionante!"
        3 "Ótimo!"
        4 "Muito bem!"
        "Ufa!")
      (str "Resposta: " final-answer))]])

(defn game-over-alert-container []
  (let [props @(rf/subscribe [:game-over-info])]
    [game-over-alert props]))

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

#?(:cljs (goog-define VERSION "dev")
   :clj (def VERSION "dev"))

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
   [:p "No modo " [:b "Bento"] ", as palavras têm " [:b "5 letras"] " e as tentativas são " [:b "palavras em geral"] "."]
   [:p "No modo " [:b "Capitu"] ", as palavras têm " [:b "6 letras"] " e as tentativas são " [:b "restritas ao conteúdo do livro "] [:em "Dom Casmurro"] "."]
   [:p "Toque ou clique no título da página para alternar entre os modos. O progresso em cada um deles é mantido."]
   [:p "O jogo funciona offline e pode ser instalado como um app pelo menu do navegador."]
   [:p [:a {:href "https://github.com/brunoadsantos/words"} "Código no GitHub"]]
   [:p [:a {:href "https://machado.mec.gov.br/obra-completa-lista/itemlist/category/23-romance"}
        "Obra de Machado de Assis em domínio público"]]
   [:p [:small "Versão " [:em (subs VERSION 0 8)]]]])

(defn bar [{:keys [attempt-number number-of-wins fraction]}]
  (let [max-size 160
        bar-size (* max-size fraction)]
    [:div.bar {:key (str "bar" attempt-number)
               :style {:width (str max-size "pt")}}
     [:div {:style {:width (str bar-size "pt")}}
      number-of-wins]]))

(defn attempts-distribution-chart [attempts-distribution]
  (when (seq attempts-distribution)
    [:<>
     [:h3 "Distribuição por número de tentativas"]
     (for [{:keys [attempt-number] :as info} attempts-distribution]
       [:div.centered-div {:key (str attempt-number)}
        (str attempt-number)
        [bar info]])]))

(defn stats []
  (let [{:keys [game-mode total-games-played total-wins attempts-distribution]} @(rf/subscribe [:stats-info])]
    [:<>
     [:h1 (game-mode->str game-mode)]
     [:h3 "Estatísticas"]
     [:p "Jogos finalizados: " total-games-played]
     [:p "Vitórias: " total-wins]
     [attempts-distribution-chart attempts-distribution]]))

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
   [title-container]
   [attempt-rows-container]
   [game-over-alert-container]
   [keyboard]])

(defn app []
  [body])
