(ns main.views
  (:require [re-frame.core :as rf]))

(defn letter-slot [{:keys [letter result idx current-row? current? last-added?]}]
  [:div
   {:key (str idx)
    :data-letter-idx idx
    :class (str "letter "
                (when current-row? "current-row ")
                (when (and current-row? current?) "current-letter ")
                (when last-added? "pulse ")
                (name result))}
   letter])

(defn attempt-row [{:keys [attempt-number]}]
  (let [row @(rf/subscribe [:attempt-row-n attempt-number])
        id (str "row" attempt-number)]
    [:div.centered-div {:key id :id id}
     (map letter-slot row)]))

(defn icon [icon-name]
  [:span.material-symbols-outlined
   (name icon-name)])

(defn button [{:keys [code text icon-name status]}]
  [(if icon-name
     :button.keyboard-btn.control
     :button.keyboard-btn) {:on-click #(rf/dispatch [:key-input code])}
   (if icon-name
     [:span [icon icon-name]]
     [:span {:class (some-> status name)}
      (str text)])])

(defn button-row
  [{:keys [row]}]
  [:div.centered-div.button-row
   (map (partial vector button) row)])

(defn keyboard []
  (let [{:keys [rows]} @(rf/subscribe [:keyboard-info])]
    [:div.keyboard
     (map (partial vector button-row) rows)]))

(def game-mode->str
  {:bento "BENTO"
   :capitu "CAPITU"})

(defn title [{:keys [game-mode]}]
  [:div.title.centered-div
   [:h1
    [:span {:on-click #(rf/dispatch [:switch-game-mode])}
     (game-mode->str game-mode)
     [:sub [icon :swap_horiz]]]]])

(defn title-container []
  (let [props {:game-mode @(rf/subscribe [:game-mode])}]
    [title props]))

(defn attempt-rows [{:keys [max-attempts]}]
  [:div.attempt-rows.centered-div
   (for [i (range max-attempts)]
     [attempt-row {:key i
                   :attempt-number i}])])

(defn attempt-rows-container []
  (let [props {:max-attempts @(rf/subscribe [:max-attempts])}]
    [attempt-rows props]))

(defn game-over-alert [{:keys [final-answer victory-attempt-number game-over? success?]}]
  [:div.centered-div.game-over
   (when game-over?
     [:span.game-over-banner.pulse
      (if success?
        (case victory-attempt-number
          0 "Ótimo chute!"
          1 "Excelente!"
          2 "Impressionante!"
          3 "Ótimo!"
          4 "Muito bem!"
          "Ufa!")
        (str "Resposta: " final-answer))])])

(defn game-over-alert-container []
  (let [props @(rf/subscribe [:game-over-info])]
    [game-over-alert props]))

(defn indicator-line [line]
  [:div {:style {:min-width "60%"
                 :text-align :left
                 :margin-left "8px"}}
   line])

(defn overlay [id content]
  (let [shown? @(rf/subscribe [:overlay-shown? id])]
    [:div.overlay.centered-div {:style {:left (if shown? "0%" "-110%")}
                                :on-click #(rf/dispatch [:set-overlay-shown id false])}
     [:div.overlay-content {:on-click (fn [event] (.stopPropagation event))}
      content]
     [:button.close-btn {:on-click #(rf/dispatch [:set-overlay-shown id false])}
      [:div [icon "close"]]]]))

#?(:cljs (goog-define VERSION "dev")
   :clj (def VERSION "dev"))

(defn boolean-pref-checkbox [{:keys [pref-id label]}]
  [:p
   [:input {:id (name pref-id)
            :type "checkbox"
            :checked (boolean (get @(rf/subscribe [:prefs]) pref-id true))
            :on-change (fn [e] (rf/dispatch [:set-pref pref-id (-> e .-target .-checked)]))}]
   [:label {:for (name pref-id)} label]])

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
   [:h3 "Configurações"]
   [boolean-pref-checkbox {:pref-id :vibrate? :label "Feedback com vibração"}]
   [:p [:a {:href "https://github.com/brunoadsantos/words"} "Código no GitHub"]]
   [:p [:a {:href "https://machado.mec.gov.br/obra-completa-lista/itemlist/category/23-romance"}
        "Obra de Machado de Assis em domínio público"]]
   [:p [:small "Versão " [:em (subs VERSION 0 8)]]]])

(defn bar [{:keys [attempt-number number-of-wins fraction]}]
  (let [max-size 160
        bar-size (* max-size fraction)]
    [:div.bar {:key (str "bar" attempt-number)
               :style {:width (str max-size "px")}}
     [:div {:style {:width (str bar-size "px")}}
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
                           :top "8px"}
                          styling)
            :on-click #(rf/dispatch [:set-overlay-shown id true])}
   [:div [icon icon-name]]])

(defn body []
  [:<>
   [overlay :about [about]]
   [overlay :stats [stats]]
   [overlay-trigger :about :info {:left "8px"}]
   [overlay-trigger :stats :leaderboard {:right "8px"}]
   [title-container]
   [game-over-alert-container]
   [attempt-rows-container]
   [keyboard]])

(defn app []
  [body])
