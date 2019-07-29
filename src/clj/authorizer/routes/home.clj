(ns authorizer.routes.home
  (:require
    [authorizer.layout :as layout]
    [clojure.java.io :as io]
    [authorizer.middleware :as middleware]
    [ring.util.http-response :as response]
    [authorizer.logic.functions :as logic.functions]))

(defn handle-transaction
  [{:keys [account transaction lastTransactions]}]
  (let [{:keys [approved] :as response} (logic.functions/authorize-transaction account transaction lastTransactions)]
    (if (approved)
      {:status 200 :body response}
      {:status 401 :body response})))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-formats]}
   ["/authorize"
      {:post
       {:summary "post a transaction to check"
        :parameters {:body
                     {:account {
                                :cardIsActive boolean?
                                :limit float?
                                :blacklist vector?
                                :isWhitelisted boolean?}
                      :transaction {
                                    :merchant string?
                                    :amount float?
                                    :time string?}
                      :LastTransactions vector?}}
        :responses {200 {:body {:approved boolean? :newLimit float? :deniedReasons vector?}}}
        :handler handle-transaction}}]])


