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

   :opts [{:option "group"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [group-name (prompt.config/get-group-name props)
                 config (api.config/read-edn (api.config/get-config-file group-name))

                 state (api.state/get-state group-name)

                 include
                 (prompt/list-checkbox "Select Services"
                                       (->> config
                                            (map (fn [[service]]
                                                   {:value (name service)
                                                    :label (name service)
                                                    :checked (get-in state [:services service :enabled] true)}))))
                 include (set include)

                 services-partial
                 (->> config
                      (filter (fn [[service]]
                                (some #{(name service)} (set include))))
                      (into {}))

                 updated-state
                 (update state :services
                         (fn [services]
                           (->> config
                                (map (fn [[service-name]]
                                       (let [previous-value (get services service-name)
                                             enabled (boolean (some #{(name service-name)} include))]
                                         [service-name (assoc previous-value :enabled enabled)])))
                                (into {}))))]

             (api.state/save-state group-name updated-state)

             (api.resolver/pull! group-name {})
             (api.executor/start-configuration! {:group-name group-name
                                                 :services services-partial})))})

(def stop-cmd
  {:command "stop"
   :description "Start a service configuration"

   :opts [{:option "group"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [group-name (prompt.config/get-group-name props)
                 services (api.config/read-edn (api.config/get-config-file group-name))]
             (api.executor/stop-configuration! {:group-name group-name
                                                :services services})))})
