(ns k16.kdev.api.proxy
  (:require
   [clj-yaml.core :as yaml]
   [clojure.java.io :as io]
   [meta-merge.core :as metamerge]))

(defn- get-proxies-projection-file [group-name]
  (let [file (io/file (System/getProperty "user.home") (str ".config/kl/proxy/kdev-" group-name ".yaml"))]
    (io/make-parents file)
    file))

(defn- route->traefik-rule [{:keys [host path-prefix]}]
  (cond-> (str "Host(`" host "`)")
    path-prefix (str " && PathPrefix(`" path-prefix "`)")))

(defn- build-routes [config]
  (let [services
        (->> (get-in config [:network :services])
             (reduce (fn [acc [service-name service-def]]
                       (->> (:endpoints service-def)
                            (reduce (fn [acc [endpoint-name endpoint]]
                                      (let [full-name (str (name service-name) "-" (name endpoint-name))]
                                        (assoc-in acc [:http :services full-name :loadbalancer :servers]
                                                  [{:url (:url endpoint)}])))
                                    acc)))

                     {}))

        routes
        (->> (get-in config [:network :routes])
             (reduce (fn [acc [route-name route]]
                       (let [service-name (keyword (:service route))
                             service (get-in config [:network :services service-name])

                             endpoint-name (or (:endpoint route)
                                               (:default-endpoint service))]

                         (assoc-in acc [:http :routers (name route-name)]
                                   {:rule (route->traefik-rule route)
                                    :service (str (name service-name) "-" (name endpoint-name))})))
                     {}))]

    (metamerge/meta-merge services routes)))

(defn write-proxy-config! [{:keys [group-name config]}]
  (let [routes (build-routes config)
        file (get-proxies-projection-file group-name)]

    (spit file (yaml/generate-string routes))))
