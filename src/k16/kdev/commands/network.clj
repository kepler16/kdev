(ns k16.kdev.commands.network
  (:require
   [k16.kdev.api.module :as api.module]
   [k16.kdev.api.proxy :as api.proxy]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
   [k16.kdev.prompt.config :as prompt.config]
   [meta-merge.core :as metamerge]
   [pretty.cli.prompt :as prompt]))

(defn- set-default-service-endpoint! [props]
  (let [group-name (prompt.config/get-group-name props)

        {:keys [modules]} (api.resolver/pull! group-name {})
        module (api.module/get-resolved-module group-name modules)

        state (api.state/get-state group-name)

        service-name
        (-> (prompt/list-select "Select Service"
                                (->> (get-in module [:network :services])
                                     (map (fn [[service-name]]
                                            {:value (name service-name)
                                             :label (name service-name)}))))
            keyword)

        service (get-in module [:network :services service-name])

        endpoint-name
        (-> (prompt/list-select "Select Default Endpoint"
                                (->> (:endpoints service)
                                     (map (fn [[endpoint-name]]
                                            {:value (name endpoint-name)
                                             :label (name endpoint-name)}))))
            keyword)

        updated-state
        (assoc-in state [:network :services service-name :default-endpoint]
                  endpoint-name)]

    (api.state/save-state group-name updated-state)

    (let [module (metamerge/meta-merge module updated-state)]
      (api.proxy/write-proxy-config! {:group-name group-name
                                      :module module}))))

(def cmd
  {:command "network"
   :description "Manage networking components"

   :subcommands [{:command "service"
                  :description "Manage network services"

                  :subcommands [{:command "set-endpoint"
                                 :description "Set the default endpoint for a service"

                                 :opts [{:option "group"
                                         :short 0
                                         :type :string}]

                                 :runs set-default-service-endpoint!}]}

                 {:command "route"
                  :description "Manage network routes"

                  :subcommands [{:command "configure"
                                 :description "Select which routes are enabled or disabled"

                                 :opts [{:option "group"
                                         :short 0
                                         :type :string}]

                                 :runs (fn [_])}

                                {:command "set-service"
                                 :description "Set the service for a route"

                                 :opts [{:option "group"
                                         :short 0
                                         :type :string}]

                                 :runs (fn [_])} {:command "set-endpoint"
                                                  :description "Set the endpoint for a route"

                                                  :opts [{:option "group"
                                                          :short 0
                                                          :type :string}]

                                                  :runs (fn [_])}]}]})