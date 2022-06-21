(ns main.events
  (:require
   [main.db :as db]
   [main.logic :as l]
   [re-frame.core :as rf]))

(defn persistable-state [db]
  (dissoc db :word-map :valid-attempts :game-mode :overlays))

(rf/reg-event-fx
 :new-game
 [(rf/inject-cofx :game-mode-from-url)
  (rf/inject-cofx :saved-game-state)]
 (fn [{:keys [db game-mode-from-url saved-game-state]} [_ {:keys [game-mode force-new?]}]]
   (let [game-mode (or game-mode (:game-mode db) game-mode-from-url)
         game-state (get saved-game-state game-mode)
         new-game (merge (db/new-game {:game-mode game-mode
                                       :game-state (when-not force-new? game-state)})
                         (select-keys game-state [:stats]))]
     {:db new-game
      :save-game-state {:game-mode game-mode
                        :game-state (persistable-state new-game)}})))

(rf/reg-event-fx
 :update-db-after-attempt-reveal
 (fn [{:keys [db]} _]
   (let [letter-results (l/get-letter-results db)
         updated-db (-> db
                        (l/get-used-letters-from-current-attempt letter-results)
                        (l/check-current-attempt letter-results)
                        (l/new-attempt)
                        (l/check-game-over))]
     {:db updated-db
      :save-game-state {:game-mode (:game-mode updated-db)
                        :game-state (persistable-state updated-db)}})))

(rf/reg-event-fx
 :letter-revealed
 (fn [{:keys [db]} [_ letter-idx]]
   (let [answer (:answer db)]
     (if (= (count answer) (inc letter-idx))
       {:fx [[:dispatch [:update-db-after-attempt-reveal]]]}
       {}))))

(rf/reg-event-fx
 :reveal-attempt
 (fn [{:keys [db]} [_ attempt-number]]
   (let [letter-results (l/get-letter-results db)]
     {:db (-> db
              (assoc :revealing? true)
              (dissoc :last-letter-position-added))
      :reveal-attempt {:letter-results letter-results
                       :attempt-number attempt-number}})))

(rf/reg-event-fx
 :key-input
 (fn [{:keys [db]} [_ k]]
   (if (or (:game-over? db) (:revealing? db))
     {}
     (case k
       :check (let [attempt-number (:attempt-number db)]
                (if (l/valid-current-attempt? db)
                  {:db (l/add-valid-attempt db)
                   :fx [[:dispatch [:reveal-attempt attempt-number]]]}
                  {:reject-attempt {:attempt-number attempt-number
                                    :attempt (get-in db [:attempts attempt-number :attempt])}}))
       :delete {:db (l/pop-from-current-attempt db)}
       {:db (l/push-to-current-attempt db k)}))))

(rf/reg-event-db
 :set-overlay-shown
 (fn [db [_ overlay-id shown?]]
   (assoc-in db [:overlays overlay-id] shown?)))