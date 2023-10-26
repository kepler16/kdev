(ns k16.kdev.commands.proxy
  (:require
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.proxy :as api.proxy]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
   [k16.kdev.prompt.config :as prompt.config]
   [pretty.cli.prompt :as prompt]))

(defn- configure-proxies! [props]
  (let [config-name (prompt.config/get-config-name props)
        services (api.config/read-edn (api.config/get-services-file config-name))

        state (api.state/get-state config-name)

        include
        (prompt/list-checkbox "Select Services to Proxy"
                              (->> services
                                   (map (fn [[service]]
                                          {:value (name service)
                                           :label (name service)
                                           :checked (get-in state [:proxies service :enabled] false)}))))
        include (set include)

        services-partial
        (->> services
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

    (api.state/save-state config-name updated-state)

    (api.resolver/pull! config-name {})
    (api.proxy/write-service-proxies! {:name config-name
                                       :services services-partial})))

(def cmd
  {:command "proxy"
   :description "Manage enabled proxies"

   :subcommands [{:command "manage"
                  :description ""

                  :opts [{:option "name"
                          :short 0
                          :type :string}]

                  :runs configure-proxies!}]})
