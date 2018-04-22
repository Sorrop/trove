(ns trove.atomic-cache
  (:require [trove.sequential-cache :as seq-cache]
            [trove.recency-cache :as rec-cache]))

(defprotocol atomic-cache
  (search [self args])
  (size   [self])
  (full?  [self])
  (store [self args output])
  (fetch [self]))

(deftype sequential-cache [atm limit policy]
  atomic-cache
  (search [self args] (get-in @atm [:mappings args]))
  (size [self] (-> @atm :mappings count))
  (full?  [self] (= (size self) limit))
  (store [self args output] (let [{:keys [mappings stored]} @atm]
                              (if (full? self)
                                (seq-cache/sequential-replace! policy atm (size self) args output)
                                (reset! atm
                                        {:mappings (assoc mappings args output)
                                         :stored   (conj stored args)}))))
  (fetch [self] atm))

(deftype recency-cache [atm space-lim policy]
  atomic-cache
  (search [self args] (when-let [content (get-in @atm [:mappings args])]
                        (rec-cache/update-recencies! policy atm args)
                        content))
  (size [self] (-> @atm :mappings count))
  (full? [self] (= (size self) space-lim))
  (store [self args output] (let [{:keys [mappings ages]} @atm]
                              (if (full? self)
                                (rec-cache/recent-replace! policy atm args output)
                                (rec-cache/recent-store! policy atm args output))))
  (fetch [self] atm))
