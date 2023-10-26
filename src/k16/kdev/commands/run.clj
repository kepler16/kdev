(ns k16.kdev.commands.run
  (:require
   [k16.kdev.api.executor :as api.executor]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.prompt.config :as prompt.config]
   [pretty.cli.prompt :as prompt]))

(set! *warn-on-reflection* true)

(def start-cmd
  {:command "start"
   :description "Start a service configuration"

   :opts [{:option "name"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [config-name (prompt.config/get-config-name props)
                 config-file (api.config/get-services-file config-name)
                 services (api.config/read-edn config-file)

                 include
                 (prompt/list-checkbox "Select Services"
                                       (->> services
                                            (map (fn [[service config]]
                                                   {:value (name service)
                                                    :label (name service)
                                                    :checked (if (boolean? (:enabled config))
                                                               (:enabled config)
                                                               true)}))))
                 include (set include)

                 services-with-include
                 (->> services
                      (filter (fn [[service]]
                                (some #{(name service)} (set include)))))

                 updated-services
                 (->> services
                      (map (fn [[service config]]
                             (let [enabled (boolean (some #{(name service)} include))]
                               [service (assoc config :enabled enabled)])))
                      (into {}))]

             (api.config/write-edn config-file updated-services)

             (api.resolver/pull! config-name {})
             (api.executor/start-configuration! {:name config-name
                                                 :services services-with-include})))})

(def stop-cmd
  {:command "stop"
   :description "Start a service configuration"

   :opts [{:option "name"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [name (prompt.config/get-config-name props)]
             (let [services (api.config/read-edn (api.config/get-services-file name))]
               (api.executor/stop-configuration! {:name name
                                                  :services services}))))})
