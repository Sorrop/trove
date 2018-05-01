(ns trove.randomized-cache)

(defn random-replace! [atm args output]
  (let [to-replace (-> @atm
                       :mappings
                       keys
                       rand-nth)
        pruned     (dissoc (:mappings @atm) to-replace)]
    (reset! atm {:mappings (assoc pruned args output)})))
