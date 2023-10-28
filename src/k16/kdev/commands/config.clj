(ns k16.kdev.commands.config
  (:require
   [clojure.edn :as edn]
   [k16.kdev.api.fs :as api.fs]
   [k16.kdev.api.resolver :as api.resolver]
   [k16.kdev.prompt.config :as prompt.config]
   [k16.kdev.prompt.editor :as prompt.editor]))

(set! *warn-on-reflection* true)

(defn- edit-config! [props]
  (let [group-name (prompt.config/get-group-name props)
        current-config (api.fs/read-edn (api.fs/get-config-file group-name))

        new-config (-> (prompt.editor/open-editor {:contents (prn-str current-config)
                                                   :filetype ".edn"})
                       edn/read-string)]

    (api.fs/write-edn (api.fs/get-config-file group-name) new-config)))

(defn- pull! [{:keys [update force] :as props}]
  (let [group-name (prompt.config/get-group-name props)
        updated? (api.resolver/pull! group-name {:update-lockfile? update
                                                 :force? force})]
    (if updated?
      (println "Services updated")
      (println "Services are all up to date"))))

(def cmd
  {:command "config"
   :description "Manage service group configurations"

   :subcommands [{:command "pull"
                  :description "Pull a service config"

                  :opts [{:option "group"
                          :short 0
                          :type :string}

                         {:option "update"
                          :default false
                          :type :with-flag}

                         {:option "force"
                          :default false
                          :type :with-flag}]

                  :runs pull!}

                 {:command "update"
                  :description "Re-resolve the latest versions of a service config. This is the same as `pull --update`"

                  :opts [{:option "group"
                          :short 0
                          :type :string}

                         {:option "force"
                          :default false
                          :type :with-flag}]

                  :runs (fn [props] (pull! (assoc props :update true)))}

                 {:command "edit"
                  :description "Edit a service configuration"

                  :opts [{:option "group"
                          :short 0
                          :type :string}]

                  :runs edit-config!}]})
