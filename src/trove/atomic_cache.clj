(ns trove.atomic-cache)

(defprotocol atomic-cache
  (search [self args])
  (size   [self])
  (full?  [self])
  (store [self args output])
  (fetch [self]))

(defmulti sequential-replace! (fn [policy atm csize args output] policy))

(defmethod sequential-replace! :fifo
  [policy atm csize args output]
  (let [{:keys [mappings stored]} @atm
        first-in         (first stored)]
    (reset! atm {:mappings (-> (dissoc mappings first-in)
                               (assoc args output))
                 :stored   (-> (subvec stored 1 csize)
                               (conj args))})))

(defmethod sequential-replace! :lifo
  [policy atm csize args output]
  (let [{:keys [mappings stored]} @atm
        first-in         (first stored)]
    (reset! atm {:mappings (-> (dissoc mappings first-in)
                               (assoc args output))
                 :stored   (-> (rest stored)
                               (conj args))})))

(defmethod sequential-replace! :default [_] "error")


(deftype sequential-cache [atm limit policy]
  atomic-cache
  (search [self args] (get-in @atm [:mappings args]))
  (size [self] (-> @atm :mappings count))
  (full?  [self] (= (size self) limit))
  (store [self args output] (let [{:keys [mappings stored]} @atm]
                              (if (full? self)
                                (sequential-replace! policy atm (size self) args output)
                                (reset! atm
                                        {:mappings (assoc mappings args output)
                                         :stored   (conj stored args)}))))
  (fetch [self] atm))


