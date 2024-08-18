(ns tasks
  (:require
   [com.biffweb.config :as config]
   [clojure.tools.build.api :as b]))

(def ^:private config (delay (config/use-aero-config {:biff.config/skip-validation true})))

(defn dev
  "Starts the app locally."
  []
  (let [{:keys [nrepl/port tasks/main-ns] :as ctx} @config]
    (spit ".nrepl-port" port)
    ((requiring-resolve (symbol (str main-ns) "-main")))))

(defn uberjar
  "Create an uberjar"
  []
  (let [{:keys [tasks/main-ns]} @config
        class-dir "target/jar/classes"
        uber-file "target/jar/app.jar"
        basis (delay (b/create-basis {:project "deps.edn"}))]
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/compile-clj {:basis @basis
                    :ns-compile [main-ns]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis @basis
             :main main-ns
             :exclude ["META-INF/license/LICENSE\\..*\\.txt"]})))

(def tasks {"dev" #'dev
            "uberjar" #'uberjar})
