(ns k16.kdev.commands.run
  (:require
   [k16.kdev.api.builder :as api.builder]
   [k16.kdev.api.executor :as api.executor]
   [k16.kdev.api.fs :as api.fs]
   [k16.kdev.api.proxy :as api.proxy]
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
                 config (api.fs/read-edn (api.fs/get-config-file group-name))

                 {:keys [modules]} (api.resolver/pull! group-name {})
                 merged-config (api.builder/merge-modules group-name config modules)

                 state (api.state/get-state group-name)

                 options (->> (:containers merged-config)
                              (map (fn [[container-name]]
                                     {:value (name container-name)
                                      :label (name container-name)
                                      :checked (get-in state [:containers container-name] true)})))
                 selected-containers (->> options
                                          (prompt/list-checkbox "Select Services")
                                          set)

                 partial-containers
                 (->> (:containers merged-config)
                      (filter (fn [[container-name]]
                                (some #{(name container-name)} selected-containers)))
                      (into {}))

                 updated-state
                 (assoc state :containers
                        (->> (:containers merged-config)
                             (map (fn [[container-name]]
                                    (let [enabled (boolean (some #{(name container-name)} selected-containers))]
                                      [container-name enabled])))
                             (into {})))

                 config-with-selection
                 (assoc merged-config :containers partial-containers)]

             (api.state/save-state group-name updated-state)

             (api.proxy/write-proxy-config! {:group-name group-name
                                             :config merged-config})
             (api.executor/start-configuration! {:group-name group-name
                                                 :config config-with-selection})))})

(def stop-cmd
  {:command "stop"
   :description "Start a service configuration"

   :opts [{:option "group"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [group-name (prompt.config/get-group-name props)
                 services (api.fs/read-edn (api.fs/get-config-file group-name))]
             (api.executor/stop-configuration! {:group-name group-name
                                                :services services})))})
