(ns trove.core)

(defprotocol cache
  (search [self args])
  (clean [self])
  (store [self args output])
  (fetch [self]))

(deftype atomic-cache [atm limit]
  cache
  (search [self args] (get @atm args))
  (clean [self] (when (= (count @atm) limit)
                  (reset! atm {})))
  (store [self args output] (do
                              (clean self)
                              (swap! atm assoc args output)))
  (fetch [self] atm))

(defn cached-fn [function limit]
  (let [cstore (atomic-cache. (atom {}) limit)]
    (with-meta
      (fn [& args]
        (or (search cstore args)
            (let [res (apply function args)]
              (store cstore args res)
              res)))
      {:cache cstore})))
