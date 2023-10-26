(ns k16.kdev.api.config
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as str]))

(set! *warn-on-reflection* true)

(defn get-relative-config-path ^java.io.File [& segments]
  (let [file (io/file (System/getProperty "user.home") ".config/kdev/" (str/join "/" (flatten segments)))]
    (io/make-parents file)
    file))

(defn get-services-file ^java.io.File [name]
  (get-relative-config-path name "services.edn"))

(defn get-lock-file ^java.io.File [name]
  (get-relative-config-path name "services.lock"))

(defn from-work-dir ^java.io.File [config-name & segments]
  (get-relative-config-path config-name ".kdev" (flatten segments)))

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

(defn list-configurations []
  (let [dir (get-relative-config-path)]
    (->> (.listFiles dir)
         (map (fn [^java.io.File file]
                (.getName file)))
         (filter (fn [name]
                   (not (str/starts-with? name ".")))))))
