(ns k16.kdev.commands.config
  (:require
   [clojure.edn :as edn]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.prompt.config :as prompt.config]
   [k16.kdev.prompt.editor :as prompt.editor]))

(set! *warn-on-reflection* true)

(defn- edit-config! [props]
  (let [name (prompt.config/get-config-name props)
        current-config (api.config/read-edn (api.config/get-config-file name))

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
                          :type :string}]

                  :runs edit-config!}

                 {:command "pull"
                  :description "Pull a service config"

                  :opts [{:option "name"
                          :short 0
                          :type :string}

                         {:option "update"
                          :default false
                          :type :with-flag}]

                  :runs (fn [{:keys [update] :as props}]
                          (let [name (prompt.config/get-config-name props)
                                updated? (api.resolver/pull! name update)]
                            (if updated?
                              (println "Service config updated")
                              (println "Service config is already up to date"))))}]})
