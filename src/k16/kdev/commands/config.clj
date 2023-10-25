(ns k16.kdev.commands.config
  (:require
   [clojure.edn :as edn]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.prompt.editor :as prompt.editor]))

(defn- edit-config! [{:keys [name]}]
  (let [current-config (api.config/read-edn (api.config/get-config-file name))

        new-config (-> (prompt.editor/open-editor {:contents (prn-str current-config)
                                                   :filetype ".edn"})
                       edn/read-string)]

    (api.config/write-edn (api.config/get-config-file name) new-config)))

(def cmd
  {:command "config"
   :description "Manage service configurations"

   :subcommands [{:command "update"
                  :description "Edit a service configuration"

                  :opts [{:option "name"
                          :short 0
                          :default :present
                          :type :string}]

                  :runs edit-config!}

                 {:command "pull"
                  :description "Pull a service config"

                  :opts [{:option "name"
                          :short 0
                          :default :present
                          :type :string}

                         {:option "update"
                          :default false
                          :type :with-flag}]

                  :runs (fn [{:keys [name update]}]
                          (let [updated? (api.resolver/pull! name update)]
                            (if updated?
                              (println "Service config updated")
                              (println "Service config is already up to date"))))}]})
