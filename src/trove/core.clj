(ns trove.core
  (:require [trove.atomic-cache :as cache]))

(defn cached [function cstore]
  (with-meta
    (fn [& args]
      (or (.search cstore args)
          (let [res (apply function args)]
            (.store cstore args res)
            res)))
    {:cache cstore}))


(defn cached-fn [function limit]
  (let [cstore (cache/->sequential-cache (atom {:mappings {}
                                                :stored   []})
                                         limit
                                         :fifo)]
    (cached function cstore)))
