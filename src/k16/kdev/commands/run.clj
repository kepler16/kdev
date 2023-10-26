(ns k16.kdev.commands.run
  (:require
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.executor :as api.executor]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
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

                 state (api.state/get-state config-name)

                 include
                 (prompt/list-checkbox "Select Services"
                                       (->> services
                                            (map (fn [[service]]
                                                   {:value (name service)
                                                    :label (name service)
                                                    :checked (get-in state [:services service :enabled] true)}))))
                 include (set include)

                 services-partial
                 (->> services
                      (filter (fn [[service]]
                                (some #{(name service)} (set include))))
                      (into {}))

                 updated-state
                 (update state :services
                         (fn [services]
                           (->> services
                                (map (fn [[service]]
                                       [service {:enabled (boolean (some #{(name service)} include))}])))))]

             (api.state/save-state config-name updated-state)

             (api.resolver/pull! config-name {})
             (api.executor/start-configuration! {:name config-name
                                                 :services services-partial})))})

(def stop-cmd
  {:command "stop"
   :description "Start a service configuration"

   :opts [{:option "name"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [name (prompt.config/get-config-name props)
                 services (api.config/read-edn (api.config/get-services-file name))]
             (api.executor/stop-configuration! {:name name
                                                :services services})))})
