(ns k16.kdev.api.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn from-config-dir ^java.io.File [& segments]
  (let [file (io/file (System/getProperty "user.home") ".config/kdev/" (str/join "/" (flatten segments)))]
    (io/make-parents file)
    file))

(defn get-config-file ^java.io.File [group-name]
  (from-config-dir group-name "config.edn"))

(defn get-lock-file ^java.io.File [group-name]
  (from-config-dir group-name "config.lock.edn"))

(defn from-work-dir ^java.io.File [config-name & segments]
  (from-config-dir config-name ".kdev" (flatten segments)))

(defn from-module-dir ^java.io.File [config-name service & segments]
  (from-work-dir config-name ".services" (name service) (flatten segments)))

(defn from-module-build-dir ^java.io.File [config-name service & segments]
  (from-module-dir config-name service ".build" segments))

(defn read-edn [^java.io.File file]
  (try
    (edn/read-string (slurp file))
    (catch Exception _ {})))

(defn write-edn [^java.io.File file data]
  (let [contents (with-out-str (pprint/pprint data))]
    (spit file contents)))

(defn list-configuration-groups []
  (let [dir (from-config-dir)]
    (->> (.listFiles dir)
         (map (fn [^java.io.File file]
                (.getName file)))
         (filter (fn [name]
                   (not (str/starts-with? name ".")))))))
