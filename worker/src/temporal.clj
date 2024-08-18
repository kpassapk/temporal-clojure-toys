(ns temporal
  (:require
   [clojure.tools.logging :as log]
   [com.biffweb.config :as config]
   [nrepl.cmdline :as nrepl-cmd]
   [ring.adapter.jetty9 :as jetty]
   [temporal.activity :refer [defactivity] :as ta]
   [temporal.client.core :as tc]
   [temporal.client.worker :as twk]
   [temporal.tls :as ttls]
   [temporal.workflow :refer [defworkflow] :as tw])
  (:gen-class))

(defactivity add-one [ctx {:keys [n]}]
  (log/info "Activity context: " ctx)
  (+ 1 n))

(defworkflow do-work [args]
  @(ta/invoke add-one args))

(defn use-temporal-worker [{:temporal/keys [target namespace cert-path key-path] :as ctx}]
 (let [ssl-context (ttls/new-ssl-context {;; :ca-path "/Users/kyle/src/tmp/temporal/worker/resources/certs/ca.pem"
                                           :cert-path cert-path
                                           :key-path key-path})
        client (tc/create-client {:target target
                                  :namespace namespace
                                  :enable-https true
                                  :ssl-context ssl-context})
        worker (twk/start client {:task-queue ::queue :ctx ctx})
        ctx (assoc ctx
                   :temporal/client client
                   :temporal/worker worker)]
    (log/info "Connected to Temporal endpoint" target "namespace" namespace "queue" ::queue)
    (update ctx :app/stop conj #(twk/stop worker))))

(defn handler [_req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "OK"})

(defn use-jetty [{:app/keys [host port handler] :as ctx}]
  (jetty/run-jetty handler
                   {:host host
                    :port port
                    :join? false})
  (log/info "Jetty running on port" (str "http://" host ":" port))
  ctx)

(def modules [])

(def components
  [config/use-aero-config
   use-temporal-worker
   use-jetty])

(def initial-system
  {:biff.config/skip-validation true
   :app/handler #'handler
   :modules #'modules})

(defonce system (atom {}))

(defn get-number [{:keys [temporal/client]}]
  (let [wf (tc/create-workflow client do-work {:task-queue ::queue})]
    wf
    (tc/start wf {:n 2})
    (tc/get-result wf)))

(comment
  (tc/create-workflow (:temporal/client @system) do-work {:task-queue ::queue})

  (use-temporal-worker @system)
  )

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
