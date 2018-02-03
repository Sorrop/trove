(ns trove.core
  (:require [trove.atomic-cache :as cache]))


(defn cached-fn [function limit]
  (let [cstore (cache/->fifo-cache (atom {:mappings {}
                                          :stored   []}) limit)]
    (with-meta
      (fn [& args]
        (or (.search cstore args)
            (let [res (apply function args)]
              (.store cstore args res)
              res)))
      {:cache cstore})))
