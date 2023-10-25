(ns k16.kdev.api.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn get-relative-config-path ^java.io.File [& segments]
  (let [file (io/file (System/getProperty "user.home") ".config/kdev/" (str/join "/" segments))]
    (io/make-parents file)
    file))

(defn get-config-file ^java.io.File [name]
  (get-relative-config-path name "services.edn"))

(defn get-lock-file ^java.io.File [name]
  (get-relative-config-path name "services.lock"))

(defn get-docker-compose-file ^java.io.File [config-name service]
  (get-relative-config-path config-name ".services" (name service) "docker-compose.yaml"))

(defn read-edn [^java.io.File file]
  (try
    (edn/read-string (slurp file))
    (catch Exception _ {})))

(defn write-edn [^java.io.File file data]
  (spit file (prn-str data)))

(defn list-configurations []
  (let [dir (get-relative-config-path)]
    (->> (.listFiles dir)
         (map (fn [^java.io.File file]
                (.getName file)))
         (filter (fn [name]
                   (not (str/starts-with? name ".")))))))
