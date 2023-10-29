(ns k16.kdev.cli
  (:require
   [cli-matic.core :refer [run-cmd]]
   [k16.kdev.commands.module :as cmd.module]
   [k16.kdev.commands.network :as cmd.network]
   [k16.kdev.commands.container :as cmd.container])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-configuration
  {:command "kdev"
   :description "A command-line interface for fetching, composing and running remote docker-compose snippets"
   :version "0.0.0"
   :subcommands (concat
                 [cmd.container/cmd]
                 (:subcommands cmd.network/cmd)
                 [cmd.module/cmd])})

(defn -main [& args]
  (run-cmd args cli-configuration))
