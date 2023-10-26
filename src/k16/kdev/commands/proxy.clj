(ns k16.kdev.commands.proxy
  (:require
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.proxy :as api.proxy]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
   [k16.kdev.prompt.config :as prompt.config]
   [pretty.cli.prompt :as prompt]))

(defn- configure-proxies! [props]
  (let [group-name (prompt.config/get-group-name props)
        config (api.config/read-edn (api.config/get-config-file group-name))

        state (api.state/get-state group-name)

        include
        (prompt/list-checkbox "Select proxies to enable"
                              (->> config
                                   (map (fn [[service]]
                                          {:value (name service)
                                           :label (name service)
                                           :checked (get-in state [:proxies service :enabled] false)}))))
        include (set include)

        services-partial
        (->> config
             (filter (fn [[service]]
                       (some #{(name service)} (set include))))
             (into {}))

        updated-state
        (update state :proxies
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
    (api.proxy/write-service-proxies! {:group-name group-name
                                       :services services-partial})))

(def cmd
  {:command "proxy"
   :description "Manage enabled proxies"

   :subcommands [{:command "manage"
                  :description ""

                  :opts [{:option "group"
                          :short 0
                          :type :string}]

                  :runs configure-proxies!}]})
