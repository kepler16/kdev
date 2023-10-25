(ns k16.kdev.api.resolver
  (:require
   [jsonista.core :as json]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.github :as api.github]
   [promesa.core :as p]
   [clojure.string :as str]))

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

(defn read-repo-file [identifier sha path]
  (let [res (api.github/request {:path (str "/repos/" identifier "/contents/" path "?ref=" sha)
                                 :headers {"Accept" "application/vnd.github.raw"}})]
    (slurp (:body res))))

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
  (let [config (api.config/read-edn (api.config/get-config-file name))
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

(defn pull!
  ([name] (pull! name false))
  ([name update-lockfile?]
   (let [{:keys [services lockfile-updated?]}
         (resolve-services {:name name
                            :update-lockfile? update-lockfile?})

         downloads (when lockfile-updated?
                     (->> services
                          (map (fn [[service dependency]]
                                 (let [{:keys [url sha]} dependency
                                       sha-short (subs sha 0 7)]
                                   (p/vthread
                                    (println (str "Downloading " url "@" sha-short))
                                    (let [contents (-> (read-repo-file url sha "docker-compose.yaml")
                                                       (str/replace "{{SHA}}" sha)
                                                       (str/replace "{{SHA_SHORT}}" sha-short))]
                                      (spit (api.config/get-docker-compose-file name service) contents))))))
                          doall))]

     (when downloads
       (doseq [download downloads]
         @download))

     lockfile-updated?)))
