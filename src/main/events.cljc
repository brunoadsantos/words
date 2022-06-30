(ns main.events
  (:require
   [main.db :as db]
   [main.logic :as l]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :new-game
 [(rf/inject-cofx :game-mode-from-url)
  (rf/inject-cofx :saved-game-state)]
 (fn [{:keys [db game-mode-from-url saved-game-state]} [_ {:keys [game-mode force-new?]}]]
   (let [game-mode (or game-mode (:game-mode db) game-mode-from-url)
         game-state (get saved-game-state game-mode)
         prefs (:prefs saved-game-state)
         new-game (merge (db/new-game {:game-mode game-mode
                                       :game-state (when-not force-new? game-state)})
                         (select-keys game-state [:stats])
                         {:prefs prefs})]
     (cond-> {:db new-game
              :save-game-state {:game-mode game-mode
                                :game-state new-game}}
       (or (not game-state) force-new?)
       (assoc :ga-event {:event-name "new-game"
                         :event-params {:game-mode game-mode}})))))

(rf/reg-event-fx
 :switch-game-mode
 (fn [{:keys [db]} _]
   (let [game-mode (:game-mode db)
         revealing? (:revealing? db)
         new-game-mode (if (= :bento game-mode) :capitu :bento)]
     (if-not revealing?
       {:ga-event {:event-name "switch-game-mode"
                   :event-params {:new-game-mode new-game-mode}}
        :fx [[:dispatch [:new-game {:game-mode new-game-mode}]]]}
       {}))))

(rf/reg-event-fx
 :update-db-after-attempt-reveal
 (fn [{:keys [db]} _]
   (let [letter-results (l/get-letter-results db)
         updated-db (-> db
                        (l/get-used-letters-from-current-attempt letter-results)
                        (l/check-current-attempt letter-results)
                        (l/new-attempt)
                        (l/check-game-over))]
     (cond-> {:db updated-db
              :save-game-state {:game-mode (:game-mode updated-db)
                                :game-state updated-db}}
       (:game-over? updated-db)
       (assoc :ga-event {:event-name "game-complete"
                         :event-params {:game-mode (:game-mode updated-db)
                                        :success (:success? updated-db)
                                        :attempts (-> updated-db :attempt-number inc)}})))))

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
   (when-not (:revealing? db)
     (let [vibrate? (get-in db [:prefs :vibrate?] true)
           fx (if (:game-over? db)
                (case k
                  :show-stats {:fx [[:dispatch [:set-overlay-shown :stats true]]]}
                  :switch-game-mode {:fx [[:dispatch [:switch-game-mode]]]}
                  :new-game {:fx [[:dispatch [:new-game {:force-new? true}]]]}
                  nil)
                (case k
                  :check (let [attempt-number (:attempt-number db)]
                           (if (l/valid-current-attempt? db)
                             {:db (l/add-valid-attempt db)
                              :fx [[:dispatch [:reveal-attempt attempt-number]]]}
                             {:reject-attempt {:attempt-number attempt-number
                                               :attempt (get-in db [:attempts attempt-number :attempt])
                                               :vibrate? vibrate?}}))
                  :delete {:db (l/pop-from-current-attempt db)}
                  :switch-game-mode {:fx [[:dispatch [:switch-game-mode]]]}
                  {:db (l/push-to-current-attempt db k)}))]
       (merge {:vibrate {:pattern :key-input
                         :vibrate? (and vibrate? (empty? (get-in fx [:reject-attempt :attempt])))}}
              fx)))))

(rf/reg-event-fx
 :set-overlay-shown
 (fn [{:keys [db]} [_ overlay-id shown?]]
   (cond-> {:db (assoc-in db [:overlays overlay-id] shown?)}
     shown?
     (assoc :ga-event {:event-name "overlay-shown"
                       :event-params {:overlay-id overlay-id}}))))

(rf/reg-event-fx
 :set-pref
 (fn [{:keys [db]} [_ pref-id value]]
   (let [updated-db (assoc-in db [:prefs pref-id] value)]
     {:db updated-db
      :save-prefs {:prefs (:prefs updated-db)}})))
