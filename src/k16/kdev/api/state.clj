(ns k16.kdev.api.state
  (:require
   [k16.kdev.api.config :as api.config]))

(defn get-state-file [config-name]
  (api.config/from-work-dir config-name "state.edn"))

(defn get-state [config-name]
  (api.config/read-edn (get-state-file config-name)))

(defn save-state [config-name state]
  (api.config/write-edn (get-state-file config-name) state))
