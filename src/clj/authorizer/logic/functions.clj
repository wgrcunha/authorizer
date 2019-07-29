(ns authorizer.logic.functions
  (:require
    [clojure.spec.alpha :as s]
    [clj-time.core :as t]
    [clj-time.coerce :as c]))

"""
1. The transaction amount should not be above limit\n
2. No transaction should be approved when the card is blocked\n
3. The first transaction shouldn't be above 90% of the limit\n
4. There should not be more than 10 transactions on the same merchant\n
5. Merchant blacklist\n
6. There should not be more than 3 transactions on a 2 minutes interval
"""

(defn approve-card-limit?
  [{:keys [limit]}
   {:keys [amount]}]
  (> limit amount))

(defn approve-card?
  [{:keys [cardIsActive]}]
  cardIsActive)

(defn approve-first-transaction?
  [{:keys [limit]}
   {:keys [amount]}
   lastTransactions]
  (or
    (> (count lastTransactions) 0)
    (<= (float (/ amount limit)) 0.9)))

(defn approve-merchant-limit?
  [{:keys [merchant]}
   lastTransactions]
  (< (count (filter (fn [transaction] (= (get transaction :merchant) merchant)) lastTransactions)) 10))

(defn approve-merchant-blacklist?
  [{:keys [blacklist]}
   {:keys [merchant]}]
  (not (contains? (set blacklist) merchant)))

(defn approve-transactions-time-limit?
  [lastTransactions]
  (<= (count (filter (fn [transaction] (> (c/to-long (get transaction :time)) (c/to-long (-> 2 t/minutes t/ago)))) lastTransactions)) 3))

(defn update-limit!
  [{:keys [approved] :as current}
   {:keys [limit]}
   {:keys [amount]}]
  (if approved
    (assoc current :newLimit (- limit amount))
    current))

(defn update-approved!
  [current response]
  (if (not response)
    (assoc current :approved false)
    current))

(defn update-deniedReasons!
  [{:keys [deniedReasons] :as current} response reason]
  (if (not response)
    (assoc current :deniedReasons (conj deniedReasons reason))
    current))

(defn check-approve?
  [current
   response
   reason]
  (->
    current
    (update-approved! response)
    (update-deniedReasons! response reason)))

(defn authorize-transaction
  [{:keys [limit] :as account} transaction lastTransactions]
  (->
    {:approved true :deniedReasons [] :newLimit limit}
    (check-approve? (approve-card-limit? account transaction) :noLimit)
    (check-approve? (approve-card? account) :cardInactive)
    (check-approve? (approve-first-transaction? account transaction lastTransactions) :firstAbove90)
    (check-approve? (approve-merchant-limit? transaction lastTransactions) :merchantLimit)
    (check-approve? (approve-merchant-blacklist? account transaction) :merchantBlackListed)
    (check-approve? (approve-transactions-time-limit? lastTransactions) :transactionRateLimit)
    (update-limit! account transaction)))