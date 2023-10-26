(ns k16.kdev.api.proxy
  (:require
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [k16.kdev.api.config :as api.config]))

(defn get-proxies-projection-file []
  (let [file (io/file (System/getProperty "user.home") ".config/kl/proxy/kdev-managed.yaml")]
    (io/make-parents file)
    file))

(defn proxy->routing-rule [{:keys [domain prefix]}]
  (cond-> (str "Host(`" domain "`)")
    prefix (str " && PathPrefix(`" prefix "`)")))

(defn project-proxies [proxies]
  (let [file (get-proxies-projection-file)
        traefik-config
        (->> proxies
             (reduce (fn [acc [name proxy]]
                       (-> acc
                           (assoc-in [:http :routers name]
                                     {:rule (proxy->routing-rule proxy)
                                      :service name})
                           (assoc-in [:http :services name :loadbalancer :servers]
                                     [{:url (:url proxy)}])))
                     {}))

        data (yaml/generate-string traefik-config)]
    (spit file data)))

(defn write-service-proxies! [{:keys [group-name services]}]
  (->> services
       (map (fn [[service-name]]
              (let [service-config (api.config/read-edn (api.config/from-module-dir group-name service-name "service.dev.edn"))]
                (->> (:kl/proxies service-config)
                     (map (fn [[proxy-name proxy]]
                            [(keyword (str group-name "-" (name service-name) "-" (name proxy-name)))
                             (merge {:url "http://host.docker.internal"} proxy)]))))))
       (apply concat)
       (into {})
       project-proxies))
