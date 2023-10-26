(ns k16.kdev.prompt.config
  (:require
   [k16.kdev.api.config :as api.config]
   [pretty.cli.prompt :as prompt]))

(set! *warn-on-reflection* true)

(defn get-group-name [{:keys [group]}]
  (if group group
      (let [groups (api.config/list-configuration-groups)]
        (prompt/list-select "Select Configuration Group" groups))))
