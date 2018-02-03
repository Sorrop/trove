(ns trove.atomic-cache)

(defprotocol atomic-cache
  (search [self args])
  (clean [self])
  (store [self args output])
  (fetch [self]))

(deftype fifo-cache [atm limit]
  atomic-cache
  (search [self args] (get-in @atm [:mappings args]))
  (clean [self] (let [{:keys [mappings
                              stored]} @atm
                      csize            (count mappings)
                      lru              (first stored)]
                  (when (= (count mappings) limit)
                    (reset! atm {:mappings (dissoc mappings lru)
                                 :stored   (subvec stored 1 csize)}))))
  (store [self args output] (let [{:keys [mappings stored]} @atm
                                  cleaned                   (clean self)]
                              (if (nil? cleaned)
                                (reset! atm
                                        {:mappings (assoc mappings args output)
                                         :stored   (conj stored args)})
                                (reset! atm (-> (assoc-in cleaned
                                                          [:mappings args] output)
                                                (assoc :stored (conj (:stored cleaned) args)))))))
  (fetch [self] atm))
