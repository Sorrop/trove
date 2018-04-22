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


(defn fifo-cached-fn [function limit]
  (let [cstore (cache/->sequential-cache (atom {:mappings {}
                                                :stored   []})
                                         limit
                                         :fifo)]
    (cached function cstore)))

(defn lifo-cached-fn [function limit]
  (let [cstore (cache/->sequential-cache (atom {:mappings {}
                                                :stored   '()})
                                         limit
                                         :lifo)]
    (cached function cstore)))

(defn lru-cached-fn [function space-lim]
  (let [cstore (cache/->recency-cache (atom {:mappings {}
                                             :ages     {}
                                             :indexed-ages (sorted-map-by <)})
                                      space-lim
                                      :lru)]
    (cached function cstore)))

(defn mru-cached-fn [function space-lim]
  (let [cstore (cache/->recency-cache (atom {:mappings {}
                                             :ages     {}
                                             :indexed-ages (sorted-map-by >)})
                                      space-lim
                                      :mru)]
    (cached function cstore)))
