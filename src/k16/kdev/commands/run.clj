(ns k16.kdev.commands.run
  (:require
   [k16.kdev.api.executor :as api.executor]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.config :as api.config]))

(def start-cmd
  {:command "start"
   :description "Start a service configuration"

   :opts [{:option "name"
           :short 0
           :default :present
           :type :string}]

   :runs (fn [{:keys [name]}]
           (api.resolver/pull! name)
           (let [services (api.config/read-edn (api.config/get-config-file name))]
             (api.executor/start-configuration! {:name name
                                                 :services services})))})

(def stop-cmd
  {:command "stop"
   :description "Start a service configuration"

   :opts [{:option "name"
           :short 0
           :default :present
           :type :string}]

   :runs (fn [{:keys [name]}]
           (api.resolver/pull! name)
           (let [services (api.config/read-edn (api.config/get-config-file name))]
             (api.executor/stop-configuration! {:name name
                                                 :services services})))})
