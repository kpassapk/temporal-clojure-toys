(ns temporal
  (:require
   [clojure.tools.logging :as log]
   [temporal.client.core :as tc]
   [temporal.client.worker :as twk]
   [nrepl.cmdline :as nrepl-cmd]
   [com.biffweb.config :as config]
   [temporal.greeter]
   [temporal.greeter-await]
   [temporal.parent-child]))

(defn use-temporal [ctx]
 (let [client (tc/create-client)
       worker (twk/start client {:task-queue ::queue :ctx ctx})
       ctx (assoc ctx
                  :temporal/client client
                  :temporal/worker worker)]
    (update ctx :app/stop conj #(twk/stop worker))))

(defn use-foo [ctx]
  (assoc ctx :foo "bar"))

(def modules [])

(def components
  [config/use-aero-config
   use-foo
   use-temporal])

(def initial-system
  {:biff.config/skip-validation true
   :modules #'modules})

(defonce system (atom {}))

(defn start []
  (let [new-system (reduce (fn [system component]
                             (log/info "starting " (str component))
                             (component system))
                           initial-system
                           components)]
    (log/info "System started.")
    (reset! system new-system)))

(defn -main []
  (let [{:keys [nrepl/args]} (start)]
    (apply nrepl-cmd/-main args)))
