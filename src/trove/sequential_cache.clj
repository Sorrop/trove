(ns trove.sequential-cache)

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



