(ns k16.kdev.api.executor
  (:require
   [babashka.process :as proc]
   [clojure.string :as str]
   [k16.kdev.api.config :as api.config]))

(set! *warn-on-reflection* true)

(defn- exec-configuration! [{:keys [group-name services direction]}]
  (let [files (->> services
                   (map (fn [[service-name]]
                          (.toString (api.config/from-module-build-dir
                                      group-name service-name "docker-compose.yaml")))))

        direction (if (seq files) direction :down)
        args (case direction
               :up ["up" "-d" "--wait" "--remove-orphans"]
               :down ["down"])]

    (proc/shell (concat
                 ["docker" "compose"
                  "--project-name" (str "kdev-" group-name)]

                 (if (seq files)
                   ["-f" (str/join "," files)]
                   [])

                 args))))

(defn start-configuration! [props]
  (exec-configuration! (assoc props :direction :up)))

(defn stop-configuration! [props]
  (exec-configuration! (assoc props :direction :down)))
