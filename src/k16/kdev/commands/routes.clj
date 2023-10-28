(ns k16.kdev.commands.routes
  (:require
   [k16.kdev.api.fs :as api.fs]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
   [k16.kdev.prompt.config :as prompt.config]
   [pretty.cli.prompt :as prompt]))

(defn- configure-routes! [props]
  (let [group-name (prompt.config/get-group-name props)
        config (api.fs/read-edn (api.fs/get-config-file group-name))

        state (api.state/get-state group-name)

        include
        (prompt/list-checkbox "Select proxies to enable"
                              (->> config
                                   (map (fn [[service]]
                                          {:value (name service)
                                           :label (name service)
                                           :checked (get-in state [:routes service :enabled] false)}))))
        include (set include)

        services-partial
        (->> config
             (filter (fn [[service]]
                       (some #{(name service)} (set include))))
             (into {}))

        updated-state
        (update state :routes
                (fn [proxies]
                  (->> include
                       (map (fn [service]
                              (let [service (keyword service)
                                    previous-value (get proxies service)
                                    enabled (boolean (some #{(name service)} include))]
                                [service (assoc previous-value :enabled enabled)])))
                       (into {}))))]

    (api.state/save-state group-name updated-state)

    (api.resolver/pull! group-name {})
    #_(api.proxy/write-service-routes! {:group-name group-name
                                      :services services-partial})))

(def cmd
  {:command "routes"
   :description "Manage host routes"

   :subcommands [{:command "update"
                  :description "Configure which routes are active"

                  :opts [{:option "group"
                          :short 0
                          :type :string}]

                  :runs configure-routes!}]})
