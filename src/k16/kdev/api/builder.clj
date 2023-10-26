(ns k16.kdev.api.builder
  (:require
   [clj-yaml.core :as yaml]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [k16.kdev.api.config :as api.config]
   [k16.kdev.api.github :as api.github]))

(set! *warn-on-reflection* true)

(defn read-repo-file [identifier sha path]
  (let [res (api.github/request {:path (str "/repos/" identifier "/contents/" path "?ref=" sha)
                                 :headers {"Accept" "application/vnd.github.raw"}})]
    (slurp (:body res))))

(defn replace-vars [contents vars]
  (->> vars
       (reduce (fn [acc [key value]]
                 (str/replace acc (str "{{" (name key) "}}") value))
               contents)))

(defn extract-docker-compose [config]
  (->> config
       (filter (fn [[key]]
                 (= "compose" (namespace key))))
       (map (fn [[key value]]
              [(name key) value]))
       (into {})
       yaml/generate-string))

(defn proxy->traefik-label [service-name proxy-name proxy]
  (let [key (str "traefik.http.routers.kdev-"
                 service-name "-" proxy-name
                 ".rule")
        value (str "Host(`" (:domain proxy) "`)")
        value (if (:prefix proxy)
                (str value " && PathPrefix(`" (:prefix proxy) "`)")
                value)]

    (str key "=" value)))

(defn build-service [service-name service root-config]
  (let [labels
        (->> (:kl/proxies service)
             (map (fn [proxy-name]
                    (let [proxy (get-in root-config [:kl/proxies proxy-name])]
                      (proxy->traefik-label (name service-name) (name proxy-name) proxy)))))]

    (-> service
        (assoc :labels labels
               :networks ["kl"]
               :dns "172.5.0.100")
        (dissoc :kl/proxies))))

(defn build-docker-compose [config]
  (let [updated-services
        (->> (:compose/services config)
             (map (fn [[name service]]
                    (if (:kl/proxies service)
                      [name (build-service name service config)]
                      [name service])))
             (into {}))]

    (->> (assoc config :compose/services updated-services)
         extract-docker-compose)))

(defn- relative-to [subpath path]
  (if subpath
    (str/join "/" [subpath path])
    path))

(defn build! [group-name service-name dependency]
  (let [{:keys [sha url subpath]} dependency
        sha-short (subs sha 0 7)]
    (println (str "Downloading " url "@" sha-short))
    (let [config (-> (read-repo-file url sha (relative-to subpath "service.dev.edn"))
                     (replace-vars {:SHA sha
                                    :SHA_SHORT sha-short})
                     edn/read-string)
          docker-compose (build-docker-compose config)]

      (doseq [file (:kdev/include config)]
        (println (str "Downloading " file "[" service-name "]"))
        (let [contents (-> (read-repo-file url sha (relative-to subpath file))
                           (replace-vars {:SHA sha
                                          :SHA_SHORT sha-short}))]
          (spit (api.config/from-module-build-dir group-name service-name file) contents)))

      (api.config/write-edn (api.config/from-module-dir group-name service-name "service.dev.edn") config)
      (spit (api.config/from-module-build-dir group-name service-name "docker-compose.yaml") docker-compose))))
