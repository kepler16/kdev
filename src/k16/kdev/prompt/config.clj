(ns k16.kdev.prompt.config
  (:require
   [k16.kdev.api.config :as api.config]
   [pretty.cli.prompt :as prompt]))

(set! *warn-on-reflection* true)

(defn get-config-name [{:keys [name]}]
  (if name name
      (let [configurations (api.config/list-configurations)]
        (prompt/list-select "Select Service Configuration"
                            configurations))))
