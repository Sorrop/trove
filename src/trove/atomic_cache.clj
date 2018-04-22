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

(deftype recency-cache [atm space-lim policy]
  atomic-cache
  (search [self args] (when-let [content (get-in @atm [:mappings args])]
                        (update-recencies! policy atm args)
                        content))
  (size [self] (-> @atm :mappings count))
  (full? [self] (= (size self) space-lim))
  (store [self args output] (let [{:keys [mappings ages]} @atm]
                              (if (full? self)
                                (recent-replace! policy atm args output)
                                (recent-store! policy atm args output))))
  (fetch [self] atm))

