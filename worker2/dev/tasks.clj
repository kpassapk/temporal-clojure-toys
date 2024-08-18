(ns tasks
  (:require
   [com.biffweb.config :as config]))

(def ^:private config (delay (config/use-aero-config {:biff.config/skip-validation true})))

(defn dev
  "Starts the app locally."
  []
  (let [{:keys [nrepl/port tasks/main-ns] :as ctx} @config]
    (spit ".nrepl-port" port)
    ((requiring-resolve (symbol (str main-ns) "-main")))))

(def tasks {"dev" #'dev})
