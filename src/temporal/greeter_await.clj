(ns temporal.greeter-await
  (:require
   [clojure.tools.logging :as log]
   [temporal.greeter :as greeter]
   [temporal.workflow :refer [defworkflow] :as w]
   [temporal.activity :refer [defactivity] :as a]
   [temporal.signals :refer [<! >!] :as s]
   [temporal.exceptions :as e]
   [slingshot.slingshot :refer [throw+]]))

(defworkflow greeter-await-workflow
  [args]
  (let [max-greetings 3
        state (atom 0)]
    (log/info "greeter-workflow:" args)

    (s/register-signal-handler!
     (fn [signal-name {:keys [workflow-id] :as args}]
       (when (= signal-name "greet")
         (log/info "got greet signal from workflow ID " workflow-id)

         (>! workflow-id ::greeting @(a/invoke greeter/greet-activity args))

         (tap> @state)

         (swap! state inc))))

    (w/await
     (fn []
       (>= @state max-greetings)))
    @state))

(defworkflow greeter-await-single-workflow
  [{:keys [greeter-workflow-id name]}]
  (let [signals (s/create-signal-chan)
        {:keys [workflow-id]} (w/get-info)]
    (>! greeter-workflow-id "greet" {:workflow-id workflow-id :name name})
    (<! signals ::greeting)))


;; Create a greeter-await workflow
;; Create a greeter-await-single workflow, passing ht ename.
;; This workflow should send a signal with start, including
;; its workflow ID. When the other workflow greets, it should
;; send a message to the first workflow. This should now be
;; waiting for the signal,
