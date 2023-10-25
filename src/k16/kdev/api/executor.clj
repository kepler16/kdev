(ns k16.kdev.api.executor
  (:require
   [babashka.process :as proc]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [k16.kdev.api.config :as api.config]))

(defn- gen-hash [n]
  (->> (repeatedly n #(rand-int 256))
       (map #(format "%02x" %))
       (apply str)))

(defn- rm-dir [^java.io.File dir]
  (when (.isDirectory dir)
    (run! rm-dir (.listFiles dir)))
  (io/delete-file dir))

(defn- get-tmp-dir []
  (io/file (System/getProperty "java.io.tmpdir")
           (str "kdev-build-" (gen-hash 5))))

(defn- exec-configuration! [{:keys [name services direction]}]
  (let [files (->> services
                   (map (fn [[service]]
                          (.toString (api.config/get-docker-compose-file name service)))))

        args (case direction
               :up ["up" "-d" "--remove-orphans"]
               :down ["down"])]

    (proc/shell (concat
                 ["docker" "compose"
                  "--project-name" (str "kdev-" name)
                  "-f" (str/join "," files)]
                 args))))

(defn start-configuration! [props]
  (exec-configuration! (assoc props :direction :up)))

(defn stop-configuration! [props]
  (exec-configuration! (assoc props :direction :down)))
