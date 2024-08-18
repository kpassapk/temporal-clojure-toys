(ns temporal2
  (:require
   [com.biffweb.config :as config]
   [nrepl.cmdline :as nrepl-cmd]
   [clojure.tools.logging :as log]
   [temporal.workflow :refer [defworkflow] :as tw]
   [temporal.activity :refer [defactivity] :as ta]
   [temporal.client.core :as tc]
   [temporal.internal.workflow :as tiw]
   [temporal.client.worker :as twk]))

(defactivity add-three [ctx {:keys [n]}]
  (log/info "Activity context: " ctx)
  (+ 3 n))

(defworkflow do-work [args]
  @(ta/invoke add-three args))

(defn use-temporal-worker [ctx]
  (let [client (tc/create-client)
        worker (twk/start client {:task-queue ::queue :ctx ctx})
        ctx (assoc ctx
                   :temporal/client client
                   :temporal/worker worker)]
    (update ctx :biff/stop conj #(twk/stop worker))))

(def modules [])

(def components
  [config/use-aero-config
   use-temporal-worker])

(def initial-system
  {:biff.config/skip-validation true
   :modules #'modules})

(defonce system (atom {}))

(defn get-number [{:keys [temporal/client]}]
  (let [wf (tc/create-workflow client do-work {:task-queue ::queue})]
    wf
    (tc/start wf {:n 2})
    (tc/get-result wf)))

(comment
  (tc/create-workflow (:temporal/client @system) do-work {:task-queue ::queue})

  (tiw/get-annotated-name do-work))

(defn start []
  (let [new-system
        (reduce (fn [system component]
                  (log/info "starting: " (str component))
                  (component system))
                initial-system
                components)]
    (reset! system new-system)))

(defn -main []
  (let [{:keys [nrepl/args]} (start)]
    (apply nrepl-cmd/-main args)))
