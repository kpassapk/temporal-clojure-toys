(ns temporal.greeter
  (:require
   [temporal.workflow :refer [defworkflow] :as w]
   [temporal.activity :refer [defactivity] :as a]
   [temporal.signals :refer [<! >!] :as s]))

(defactivity greet-activity
  [ctx {:keys [name] :as args}]
  (str "Hi, " name))

(defworkflow greeter-stateful-workflow
  [args]
  (let [max-greetings 3
        state (atom 0)]

    (s/register-signal-handler!
     (fn [signal-name {:keys [workflow-id] :as args}]
       (when (= signal-name "greet")
         (>! workflow-id ::greeting @(a/invoke greet-activity args))

         (tap> @state)

         (swap! state inc))))

    (w/await
     (fn []
       (>= @state max-greetings)))
    @state))

(defworkflow greeting-workflow
  [{:keys [greeter-workflow-id name]}]
  (let [signals (s/create-signal-chan)
        {:keys [workflow-id]} (w/get-info)]
    (>! greeter-workflow-id "greet" {:workflow-id workflow-id :name name})
    (<! signals ::greeting)))
