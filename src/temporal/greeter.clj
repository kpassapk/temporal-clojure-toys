(ns temporal.greeter
  (:require
   [clojure.tools.logging :as log]
   [temporal.workflow :refer [defworkflow]]
   [temporal.activity :refer [defactivity] :as a]))

(defactivity greet-activity
  [ctx {:keys [name] :as args}]
  (log/info "greet-activity:" args)
  (str "Hi, " name))

(defworkflow greeter-workflow
  [args]
  (log/info "greeter-workflow:" args)
  @(a/invoke greet-activity args))

