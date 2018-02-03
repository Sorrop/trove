(ns trove.core)

(defprotocol cache
  (search [self args])
  (clean [self])
  (store [self args output])
  (fetch [self]))

(deftype atomic-cache [atm limit]
  cache
  (search [self args] (get-in @atm [:mappings args]))
  (clean [self] (let [{:keys [mappings
                              stored]} @atm
                      lru              (first stored)]
                  (when (= (count mappings) limit)
                    (reset! atm {:mappings (dissoc mappings lru)
                                 :stored   (rest stored)}))))
  (store [self args output] (let [{:keys [mappings stored]} @atm]
                              (clean self)
                              (reset! atm
                                      {:mappings (assoc mappings args output)
                                       :stored   (conj stored args)})))
  (fetch [self] atm))

(defn cached-fn [function limit]
  (let [cstore (atomic-cache. (atom {:mappings {}
                                     :stored   []}) limit)]
    (with-meta
      (fn [& args]
        (or (search cstore args)
            (let [res (apply function args)]
              (store cstore args res)
              res)))
      {:cache cstore})))
