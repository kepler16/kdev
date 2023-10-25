(ns k16.kdev.cli
  (:require
   [cli-matic.core :refer [run-cmd]]
   [k16.kdev.commands.config :as cmd.config]
   [k16.kdev.commands.run :as cmd.run])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-configuration
  {:command "kdev"
   :description "A command-line interface for fetching, composing and running remote docker-compose snippets"
   :version "0.0.0"
   :subcommands [cmd.run/start-cmd cmd.run/stop-cmd
                 cmd.config/cmd]})

(defn -main [& args]
  (run-cmd args cli-configuration))
