(ns trove.core-test
  (:require [clojure.test :refer :all]
            [trove.core :refer :all]
            #_[trove.atomic-cache :refer :all]
            #_[trove.sequential-cache :refer :all]
            #_[trove.recency-cache :refer :all]))

(defn inspect-cached [c-fun]
  (-> (meta c-fun)
      :cache
      (.fetch)
      deref))

(defn size-of-cache [c-fun]
  (-> (inspect-cached c-fun)
      :mappings
      count))

(defn peek-stored [c-fun]
  (-> (inspect-cached c-fun)
      :stored
      first))

(defn last-lifo [c-fun]
  (-> (inspect-cached c-fun)
      :stored
      last))

(defn hypotenuse
  [a b]
  (java.lang.Math/sqrt (+ (* a a) (* b b))))

(def cached-hyp-fifo
  (fifo-cached-fn hypotenuse 100))

(def cached-hyp-lru
  (lru-cached-fn hypotenuse 100))

(defn fact [n]
  (if (= 1 n)
    1
    (*' n (fact (dec n)))))

(def cached-fact
  (fifo-cached-fn fact 100))

(defn cached? [c-fn & args]
  (let [res (apply c-fn args)]
    (= (-> (meta c-fn)
           :cache
           (.search args))
       res)))

(defn apply-seq [c-fun input]
  (doseq [args input]
    (apply c-fun args)))

(defn fifo-respects-limit?
  [fun limit input excess]
  (let [c-fun (fifo-cached-fn fun limit)]
    (apply-seq c-fun input)
    (apply c-fun excess)
    (= limit (size-of-cache c-fun))))

(defn lifo-respects-limit?
  [fun limit input excess]
  (let [c-fun (lifo-cached-fn fun limit)]
    (apply-seq c-fun input)
    (apply c-fun excess)
    (= limit (size-of-cache c-fun))))

(defn fifo?
  [fun limit input excess]
  (let [c-fun        (fifo-cached-fn fun limit)
        fin          (first input)
        first-in?    (do (apply-seq c-fun input)
                         (= fin (peek-stored c-fun)))
        first-out?   (do (apply c-fun excess)
                         (not= fin (peek-stored c-fun)))]
    (and first-in? first-out?)))

(defn lifo?
  [fun limit input excess]
  (let [c-fun        (lifo-cached-fn fun limit)
        lin          (last input)
        last-in?    (do (apply-seq c-fun input)
                        (= lin (peek-stored c-fun)))
        first-out?   (do (apply c-fun excess)
                         (not= lin (peek-stored c-fun)))]
    (and last-in? first-out?)))

(defn discarded?
  [cache value]
  (and (nil? (get (:mappings cache) value))
       (nil? (get (:ages cache) value))
       (nil? (get (set (vals (:indexed-ages cache)))
                  value))))

(defn lru-simple?
  [fun limit input excess]
  (let [c-fun         (lru-cached-fn fun limit)
        lru           (first input)
        cache         (do (apply-seq c-fun input)
                          (apply c-fun excess)
                          (inspect-cached c-fun))]
    (discarded? cache lru)))

(defn mru-simple?
  [fun limit input excess]
  (let [c-fun         (mru-cached-fn fun limit)
        mru           (last input)
        cache         (do (apply-seq c-fun input)
                          (apply c-fun excess)
                          (inspect-cached c-fun))]
    (discarded? cache mru)))

(comment
  (-> (meta cached-hyp)
      :cache
      (.search [3 4])))

(deftest basic-test
  (testing "Test sequential caching facilities"
    (is (cached? cached-hyp-fifo 3 4))
    (is (fifo-respects-limit? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))
    (is (lifo-respects-limit? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))
    (is (fifo? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))
    (is (lifo? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))))

(deftest recency-test
  (testing "Recency testing"
    (is (cached? cached-hyp-lru 3 4))
    (is (lru-simple? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))
    (is (mru-simple? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))))
