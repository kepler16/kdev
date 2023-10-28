(ns k16.kdev.api.executor
  (:require
   [babashka.process :as proc]
   [clj-yaml.core :as yaml]
   [k16.kdev.api.fs :as api.fs]
   [meta-merge.core :as metamerge]))

(set! *warn-on-reflection* true)

(defn- build-docker-compose [config]
  (let [base (cond-> {:networks {:kl {:external true}}}
               (:volumes config) (assoc :volumes (:volumes config)))

        containers
        (->> (:containers config)
             (map (fn [[container-name container]]
                    [container-name
                     (metamerge/meta-merge {:networks {:kl {}}
                                            :dns "172.5.0.100"}
                                           container)]))

             (into {}))]

    (cond-> base
      (seq containers) (assoc :services containers))))

(defn- exec-configuration! [{:keys [group-name config direction]}]
  (let [compose-data (build-docker-compose config)
        compose-file (api.fs/from-work-dir group-name "docker-compose.yaml")

        direction (if (:services compose-data) direction :down)

        args (case direction
               :up ["-f" (.toString compose-file) "up" "-d" "--remove-orphans"]
               :down ["down"])]

    (spit compose-file (yaml/generate-string compose-data))

    (try
      (proc/shell (concat
                   ["docker" "compose"
                    "--project-name" (str "kdev-" group-name)]

                   args))
      (catch Exception _))))

(defn start-configuration! [props]
  (exec-configuration! (assoc props :direction :up)))

(defn stop-configuration! [props]
  (exec-configuration! (assoc props :direction :down)))
