(ns k16.kdev.api.resolver
  (:require
   [jsonista.core :as json]
   [k16.kdev.api.builder :as api.builder]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.github :as api.github]
   [promesa.core :as p]))

(set! *warn-on-reflection* true)

(def ?Dependency
  [:map
   [:url :string]
   [:sha {:optional true} :string]
   [:ref {:optional true} :string]
   [:template-params {:optional true} [:map {:closed false}]]])

(defn get-commit-for-ref [identifier ref]
  (let [res (api.github/request {:path (str "/repos/" identifier "/commits/" ref)})
        data (-> res
                 :body
                 (json/read-value json/keyword-keys-object-mapper))]
    (:sha data)))

(defn resolve-service-sha [{:keys [url sha ref]
                            :or {ref "master"}}]
  (if sha
    {:url url :sha sha}
    (do
      (println (str "Resolving " url))
      {:url url
       :sha (get-commit-for-ref url ref)
       :ref ref})))

(defn resolve-services [{:keys [name update-lockfile?]}]
  (let [config (api.config/read-edn (api.config/get-services-file name))
        lock (api.config/read-edn (api.config/get-lock-file name))

        services
        (->> config
             (map (fn [[service dependency]]
                    (p/vthread
                     (let [{:keys [sha ref] :as lock-entry} (get lock service)]
                       (if (or (not sha)
                               (and (:sha dependency) (not= (:sha dependency) sha))
                               (and (:ref dependency) (not= (:ref dependency) ref))
                               update-lockfile?)
                         [service (resolve-service-sha dependency)]
                         [service lock-entry])))))
             doall
             (map (fn [promise] @promise))
             (into {}))

        lockfile-updated? (not= services lock)]

    (when lockfile-updated?
      (spit (api.config/get-lock-file name) services))

    {:services services
     :lockfile-updated? lockfile-updated?}))

(defn pull! [name {:keys [update-lockfile? force?]}]
  (let [{:keys [services lockfile-updated?]}
        (resolve-services {:name name
                           :update-lockfile? update-lockfile?})

        downloads (when (or lockfile-updated? force?)
                    (->> services
                         (map (fn [[service dependency]]
                                (p/vthread
                                 (api.builder/build! name (clojure.core/name service) dependency))))

                         doall))]

    (when downloads
      (doseq [download downloads]
        @download))

    lockfile-updated?))
