(ns trove.recency-cache)


(def ru-policy->compr
  {:lru > :mru <})

(defn recent-replace!
  [policy atm args output]
  (let [{:keys [mappings
                ages
                indexed-ages]} @atm
        comparator             (get ru-policy->compr policy)
        [recent-age
         recent-args]          (first indexed-ages)
        indexed-ages-without   (->> (dissoc indexed-ages recent-age)
                                    (reduce (fn [acc [k v]]
                                              (assoc acc (inc k) v))
                                            (sorted-map-by comparator)))
        mappings-without       (dissoc mappings recent-args)
        ages-without           (->> (dissoc ages recent-args)
                                    seq
                                    (reduce (fn [acc [k v]]
                                              (assoc acc k (inc v)))
                                            {}))]
    (reset! atm
            {:mappings     (assoc mappings-without args output)
             :ages         (assoc ages-without args 0)
             :indexed-ages (assoc indexed-ages-without 0 args)})))

(defn recent-store! [policy atm args output]
  (let [{:keys [mappings
                ages
                indexed-ages]} @atm
        comparator             (get ru-policy->compr policy)
        new-mappings           (assoc mappings args output)
        new-ages               (->> (seq ages)
                                    (reduce (fn [acc [k v]]
                                              (assoc acc k (inc v)))
                                            {}))
        new-indexed-ages       (-> (reduce (fn [acc [k v]]
                                             (assoc acc (inc k) v))
                                           (sorted-map-by comparator)
                                           indexed-ages)
                                   (assoc 0 args))]
    (reset! atm
            {:mappings new-mappings
             :ages     (assoc new-ages args 0)
             :indexed-ages new-indexed-ages})))

(defn update-recencies!
  [policy atm args]
  (let [{:keys [mappings
                ages
                indexed-ages]} @atm
        comparator             (get ru-policy->compr policy)
        new-ages               (->> (seq ages)
                                    (reduce (fn [acc [k v]]
                                              (if (= k args)
                                                (assoc acc k v)
                                                (assoc acc k (inc v))))
                                            {}))
        new-indexed-ages       (let [arg-age (get ages args)]
                                 (if (get indexed-ages arg-age)
                                   (-> (->> (dissoc indexed-ages arg-age)
                                            (reduce (fn [acc [k v]]
                                                      (assoc acc (inc k) v))
                                                    (sorted-map-by comparator)))
                                       (assoc (get new-ages args) args))
                                   (-> (reduce (fn [acc [k v]]
                                                 (assoc acc (inc k) v))
                                               (sorted-map-by comparator))
                                       (assoc (get new-ages args) args))))]
    (reset! atm {:mappings mappings
                 :ages new-ages
                 :indexed-ages new-indexed-ages})))
