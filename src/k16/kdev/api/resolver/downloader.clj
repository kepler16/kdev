(ns k16.kdev.api.resolver.downloader
  (:require
   [cli-matic.utils :as cli.util]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [k16.kdev.api.fs :as api.config]
   [k16.kdev.api.github :as api.github]
   [promesa.core :as p]))

(set! *warn-on-reflection* true)

(defn- relative-to [subpath path]
  (if subpath
    (str/join "/" [subpath path])
    path))

(defn- read-repo-file [identifier sha path]
  (let [res (api.github/request {:path (str "/repos/" identifier "/contents/" path "?ref=" sha)
                                 :headers {"Accept" "application/vnd.github.raw"}})]

    (when (not= 200 (:status res))
      (println (str "Failed to pull " identifier "@" sha "/" path))
      (cli.util/exit! (:body res) 1))

    (slurp (:body res))))

(defn- replace-vars [contents vars]
  (->> vars
       (reduce (fn [acc [key value]]
                 (str/replace acc (str "{{" (name key) "}}") value))
               contents)))

(defn download-remote-module! [{:keys [group-name module-name module]}]
  (let [{:keys [sha url subdir]
         :or {subdir ".kdev"}} module
        sha-short (subs sha 0 7)

        build-dir (-> (api.config/from-module-build-dir group-name module-name)
                      .toString)

        vars {:SHA sha
              :SHA_SHORT sha-short
              :DIR build-dir}]

    (println (str "Downloading " url "@" sha-short))

    (let [config (-> (read-repo-file url sha (relative-to subdir "module.edn"))
                     (replace-vars vars)
                     edn/read-string)]

      @(p/all
        (->> (:include config)
             (map (fn [file]
                    (p/vthread
                     (println (str "Downloading " file " [" module-name "]"))
                     (let [contents (-> (read-repo-file url sha (relative-to subdir file))
                                        (replace-vars vars))]
                       (spit (io/file build-dir file) contents)))))))

      (api.config/write-edn (api.config/from-module-dir group-name module-name "module.edn") config))))
