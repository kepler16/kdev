(ns k16.kdev.api.state
  (:require
   [k16.kdev.api.config :as api.config]))

(defn get-state-file [group-name]
  (api.config/from-work-dir group-name "state.edn"))

(defn get-state [group-name]
  (api.config/read-edn (get-state-file group-name)))

(defn save-state [group-name state]
  (api.config/write-edn (get-state-file group-name) state))
