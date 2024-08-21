(ns repl
  (:require [temporal :as main]
            [temporal.greeter :as greeter]
            [temporal.parent-child :as parent-child]
            [temporal.client.core :as c]
            [portal.api :as p])
  (:import (java.util UUID)))

(defn get-context []
  @main/system)

(defn with-greeter
  "Returns a context with a greeter workflow.
  Pass an an optional ID if you want this greeter to survive program restarts."

  [{:keys [temporal/client] :as ctx} &

   {:keys [id] :or {id (-> (UUID/randomUUID) str)}}]

  (let [wf (c/create-workflow client greeter/greeter-stateful-workflow {:task-queue ::main/queue
                                                                        :workflow-id id})
        _ (c/signal-with-start wf ::no-op {} {})]
    (assoc ctx ::greeter/workflow-id id)))

(defn greet [{:keys [temporal/client]
              ::greeter/keys [workflow-id]} name]
    (let [greet (c/create-workflow client greeter/greeting-workflow {:task-queue ::main/queue})
          _ (c/start greet {:greeter-workflow-id workflow-id :name name})]
      @(c/get-result greet)))

(comment

  (def p (p/open)) ; Open a new inspector

  (add-tap #'p/submit) ; Add portal as a tap> target

  (def id "21bc987c-af0d-46ff-bb53-bc6d5599ee49")

  ;; Option 1: Automatically generate ID.
  ;; This will not survive restarts.
  (def greeter (with-greeter (get-context)))

  ;; Option 2: Pass in a known ID.
  ;; This should survive restarts.
  (def greeter (with-greeter (get-context) {:id id}))

  (greet greeter "Martin")

  ;; Greeter

  ;; From Temporal tests
  ;; Parent / child

  )
