(ns repl
  (:require [temporal :as main]
            [temporal.greeter :as greeter]
            [temporal.greeter-await :as game]
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

  (let [wf (c/create-workflow client game/greeter-await-workflow {:task-queue ::main/queue
                                                                  :workflow-id id})
        _ (c/signal-with-start wf ::no-op {} {})]
    (assoc ctx ::game/workflow-id id)))

(defn greet [{:keys [temporal/client]
              ::game/keys [workflow-id]} name]
    (let [greet (c/create-workflow client game/greeter-await-single-workflow {:task-queue ::main/queue})
          _ (c/start greet {:greeter-workflow-id workflow-id :name name})]
      @(c/get-result greet)))

(comment

  (tap> 3)

  (def p (p/open)) ; Open a new inspector

  (add-tap #'p/submit) ; Add portal as a tap> target

  ;; Run this in REPL and paste ID below
  (-> (UUID/randomUUID) str)

  (def id "21bc987c-af0d-46ff-bb53-bc6d5599ee49")

  ;; Option 1: Automatically generate ID.
  ;; This will not survive restarts.
  (def greeter (with-greeter (get-context)))

  ;; Option 2: Pass in a known ID.
  ;; This should survive restarts.
  (def greeter (with-greeter (get-context) {:id id}))

  (greet greeter "Martin")

  ;; Greeter
  (let [client (:temporal/client (get-context))
      workflow (c/create-workflow client greeter/greeter-workflow {:task-queue ::main/queue})]
    (c/start workflow {:name "Bob"}))

  ;; Each greeting separately
  (let [client (:temporal/client (get-context))
        wf (c/create-workflow client game/greeter-await-workflow {:task-queue ::main/queue
                                                                           :workflow-id id})
        greet (c/create-workflow client game/greeter-await-single-workflow {:task-queue ::main/queue})
        _ (c/signal-with-start wf ::dummy {} {})]
    (c/start greet {:greeter-workflow-id id :name "Kyle"})
    @(c/get-result greet))


  ;; From Temporal tests
  ;; Parent / child
  (let [client (:temporal/client (get-context))
        workflow (c/create-workflow client parent-child/parent-workflow {:task-queue ::main/queue})]
      (c/start workflow {:names ["Bob" "George" "Fred"]})
      @(c/get-result workflow))


  )
