(ns k16.kdev.commands.run
  (:require
   [k16.kdev.api.executor :as api.executor]
   [k16.kdev.api.fs :as api.fs]
   [k16.kdev.api.module :as api.module]
   [k16.kdev.api.proxy :as api.proxy]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.api.state :as api.state]
   [k16.kdev.prompt.config :as prompt.config]
   [meta-merge.core :as metamerge]
   [pretty.cli.prompt :as prompt]))

(set! *warn-on-reflection* true)

(def run-cmd
  {:command "run"
   :description "Select containers in a module to run"

   :opts [{:option "group"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [group-name (prompt.config/get-group-name props)

                 {:keys [modules]} (api.resolver/pull! group-name {})
                 module (api.module/get-resolved-module group-name modules)

                 state (api.state/get-state group-name)

                 options (->> (:containers module)
                              (map (fn [[container-name]]
                                     {:value (name container-name)
                                      :label (name container-name)
                                      :checked (get-in state [:containers container-name :enabled] true)})))
                 selected-containers (->> options
                                          (prompt/list-checkbox "Select Services")
                                          set)

                 updated-state
                 (assoc state :containers
                        (->> (:containers module)
                             (map (fn [[container-name]]
                                    (let [enabled (boolean (some #{(name container-name)} selected-containers))]
                                      [container-name {:enabled enabled}])))
                             (into {})))]

             (api.state/save-state group-name updated-state)

             (let [module (metamerge/meta-merge module updated-state)]
               (api.proxy/write-proxy-config! {:group-name group-name
                                               :module module})
               (api.executor/start-configuration! {:group-name group-name
                                                   :module module}))))})

(def stop-cmd
  {:command "down"
   :description "Stop all running containers for a module"

   :opts [{:option "group"
           :short 0
           :type :string}]

   :runs (fn [props]
           (let [group-name (prompt.config/get-group-name props)
                 services (api.fs/read-edn (api.fs/get-root-module-file group-name))]
             (api.executor/stop-configuration! {:group-name group-name
                                                :services services})))})
