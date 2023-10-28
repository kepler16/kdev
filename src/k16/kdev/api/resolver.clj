(ns k16.kdev.api.resolver
  (:require
   [cli-matic.utils :as cli.util]
   [jsonista.core :as json]
   [k16.kdev.api.fs :as api.fs]
   [k16.kdev.api.github :as api.github]
   [k16.kdev.api.resolver.downloader :as resolver.downloader]
   [promesa.core :as p]))

(set! *warn-on-reflection* true)

(defn- get-commit-for-ref [identifier ref]
  (let [res (api.github/request {:path (str "/repos/" identifier "/commits/" ref)})
        data (-> res
                 :body
                 (json/read-value json/keyword-keys-object-mapper))]

    (when (not= 200 (:status res))
      (println (str "Failed to resolve " identifier "@" ref))
      (cli.util/exit! (:message data) 1))

    (:sha data)))

(defn- resolve-dependency-sha [{:keys [url sha ref subdir]
                                :or {ref "master"}}]

  (when-not sha
    (println (str "Resolving " url (if subdir (str "/" subdir) ""))))

  (let [sha (if sha sha (get-commit-for-ref url ref))]
    (cond-> {:url url :sha sha :ref ref}
      subdir (assoc :subdir subdir))))

(defn- resolve-modules [{:keys [config lock force-resolve?]}]
  (->> (:modules config)
       (map (fn [[service-name dependency]]
              (p/vthread
               (let [{:keys [sha ref subdir] :as lock-entry} (get lock service-name)

                     should-resolve?
                     (or (not sha)

                         (and (:sha dependency) (not= (:sha dependency) sha))
                         (and (:ref dependency) (not= (:ref dependency) ref))
                         (and (:subdir dependency) (not= (:subdir dependency) subdir))

                         force-resolve?)]
                 (if should-resolve?
                   [service-name (resolve-dependency-sha dependency)]
                   [service-name lock-entry])))))
       doall
       (map (fn [promise] @promise))
       (into {})))

(defn pull! [group-name {:keys [update-lockfile? force?]}]
  (let [config (api.fs/read-edn (api.fs/get-root-module-file group-name))
        lock (api.fs/read-edn (api.fs/get-lock-file group-name))

        modules (resolve-modules {:config config
                                  :lock lock
                                  :force-resolve? update-lockfile?})

        lockfile-updated? (not= modules lock)

        downloads (when (or lockfile-updated? force?)
                    (->> modules
                         (map (fn [[module-name module]]
                                (p/vthread
                                 (resolver.downloader/download-remote-module!
                                  {:group-name group-name
                                   :module-name (name module-name)
                                   :module module}))))

                         doall))]

    (when lockfile-updated?
      (api.fs/write-edn (api.fs/get-lock-file group-name) modules))

    (when downloads
      (doseq [download downloads]
        @download))

    {:modules modules
     :lockfile-updated? lockfile-updated?}))
