(ns authorizer.test.logic.functions
  (:require
    [clojure.test :refer :all]
    [authorizer.logic.functions :as logic.functions]
    [clj-time.core :as t]))


(def active-card-1k-limit {
                           :limit 1000
                           :blacklist []
                           :cardIsActive true
                           :isWhiteListed false})

(def active-card-1k-limit-with-blacklist {
                                          :limit 1000
                                          :blacklist ["padaria"]
                                          :cardIsActive true
                                          :isWhiteListed false})

(def inactive-card-1k-limit {
                             :limit 1000
                             :blacklist []
                             :cardIsActive false
                             :isWhiteListed false})

(def transaction-padaria-100 {
                              :merchant "padaria"
                              :amount 100
                              :time ""})

(def transaction-padaria-950 {
                              :merchant "padaria"
                              :amount 950
                              :time ""})

(def transaction-padaria-1100 {
                               :merchant "padaria"
                               :amount 1100
                               :time ""})

(def multiple-times-transactions (repeat 4 {:merchant "padaria" :time (-> 1 t/minutes t/ago)}))

(def multiple-padaria-transactions (repeat 11 {:merchant "padaria" :time (-> 1 t/minutes t/ago)}))

(deftest test-approve-card-limit?

  (testing "card has limit available for this transaction"
    (let [response (logic.functions/approve-card-limit? active-card-1k-limit transaction-padaria-100)]
      (is (= true response))))

  (testing "card has no limit available for this transaction"
    (let [response (logic.functions/approve-card-limit? active-card-1k-limit transaction-padaria-1100)]
      (is (= false response)))))

(deftest test-approve-card?

  (testing "card is active"
    (let [response (logic.functions/approve-card? active-card-1k-limit)]
      (is (= true response))))

  (testing "card has no limit available for this transaction"
    (let [response (logic.functions/approve-card? inactive-card-1k-limit)]
      (is (= false response)))))

(deftest test-approve-first-transaction?

  (testing "first transaction using less than 90% of the limit"
    (let [response (logic.functions/approve-first-transaction? active-card-1k-limit transaction-padaria-100 [])]
      (is (= true response))))

  (testing "first transaction using more than 90% of the limit"
    (let [response (logic.functions/approve-first-transaction? active-card-1k-limit transaction-padaria-950 [])]
      (is (= false response))))

  (testing "non first transaction"
    (let [response (logic.functions/approve-first-transaction? active-card-1k-limit transaction-padaria-950 [transaction-padaria-950])]
      (is (= true response)))))

(deftest test-approve-merchant-limit?

  (testing "aprove with 10 transactions or less"
    (let [response (logic.functions/approve-merchant-limit? transaction-padaria-950 [])]
      (is (= true response))))

  (testing "deny with 11 transactions from the same merchant"
    (let [response (logic.functions/approve-merchant-limit? transaction-padaria-950 multiple-padaria-transactions)]
      (is (= false response)))))

(deftest check-approve-merchant-blacklist?

  (testing "merchant not on blacklist"
    (let [response (logic.functions/approve-merchant-blacklist? active-card-1k-limit transaction-padaria-950)]
      (is (= true response))))

  (testing "merchant on blacklist"
    (let [response (logic.functions/approve-merchant-blacklist? active-card-1k-limit-with-blacklist transaction-padaria-950)]
      (is (= false response)))))

(deftest check-approve-transactions-time-limit?

  (testing "less than 3 transactions on 2 minutes"
    (let [response (logic.functions/approve-transactions-time-limit? [])]
      (is (= true response))))

  (testing "more than 3 transactions on 2 minutes"
    (let [response (logic.functions/approve-transactions-time-limit? multiple-times-transactions)]
      (is (= false response)))))

(deftest check-authorize-transaction

  (testing "must approve this transaction"
    (let [response (logic.functions/authorize-transaction active-card-1k-limit transaction-padaria-100 [])]
      (is (= {:approved true, :deniedReasons [], :newLimit 900} response))))

  (testing "must deny this transaction"
    (let [response (logic.functions/authorize-transaction active-card-1k-limit transaction-padaria-950 [])]
      (is (= {:approved false, :deniedReasons [:firstAbove90] :newLimit 1000} response))))

  (testing "multiple deny for this transaction"
    (let [response (logic.functions/authorize-transaction active-card-1k-limit transaction-padaria-1100 multiple-padaria-transactions)]
      (is (= {:approved false, :deniedReasons [:noLimit :merchantLimit :transactionRateLimit] :newLimit 1000} response)))))