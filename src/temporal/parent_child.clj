(ns temporal.parent-child
  (:require
   [clojure.tools.logging :as log]
   [temporal.workflow :refer [defworkflow] :as w]
   [temporal.activity :refer [defactivity] :as a]))

(defactivity child-greeter-activity
  [ctx {:keys [name] :as args}]
  (log/info "greet-activity:" args)
  (str "Hi, " name))

(defworkflow child-workflow
  [{:keys [names] :as args}]
  (log/info "child-workflow:" args)
  (for [name names]
    @(a/invoke child-greeter-activity {:name name})))

(defworkflow parent-workflow
  [args]
  (log/info "parent-workflow:" args)
  @(w/invoke child-workflow args {:retry-options {:maximum-attempts 1} :task-queue :temporal/queue}))
