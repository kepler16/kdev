(ns k16.kdev.api.builder
  (:require
   [k16.kdev.api.fs :as api.fs]
   [meta-merge.core :as metamerge]))

(set! *warn-on-reflection* true)

(defn merge-modules [group-name root-config modules]
  (let [merged
        (->> modules
             (reduce (fn [acc [module-name]]
                       (let [config-file (api.fs/from-module-dir group-name module-name "config.edn")
                             config (api.fs/read-edn config-file)]
                         (metamerge/meta-merge acc config)))
                     {}))]
    (metamerge/meta-merge merged root-config)))
