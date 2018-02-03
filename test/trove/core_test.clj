(ns trove.core-test
  (:require [clojure.test :refer :all]
            [trove.core :refer :all]))

(defn inspect-cached [c-fun]
  (-> (meta c-fun)
      :cache
      (fetch)
      deref))

(defn size-of-cache [c-fun]
  (-> (inspect-cached c-fun)
      :mappings
      count))

(defn peek-stored [c-fun]
  (-> (inspect-cached c-fun)
      :stored
      first))

(defn hypotenuse
  [a b]
  (java.lang.Math/sqrt (+ (* a a) (* b b))))

(def cached-hyp
  (cached-fn hypotenuse 100))

(defn fact [n]
  (if (= 1 n)
    1
    (*' n (fact (dec n)))))

(def cached-fact
  (cached-fn fact 100))

(defn cached? [c-fn & args]
  (let [res (apply c-fn args)]
    (= (-> (meta c-fn)
           :cache
           (search args))
       res)))

(defn apply-seq [c-fun input]
  (doseq [args input]
    (apply c-fun args)))

(defn respects-limit?
  [fun limit input excess]
  (let [c-fun (cached-fn fun limit)]
    (apply-seq c-fun input)
    (apply c-fun excess)
    (= limit (size-of-cache c-fun))))

(defn fifo?
  [fun limit input excess]
  (let [c-fun        (cached-fn fun limit)
        fin          (first input)
        first-in?    (do (apply-seq c-fun input)
                         (= fin (peek-stored c-fun)))
        first-out?   (do (apply c-fun excess)
                         (not= fin (peek-stored c-fun)))]
    (and first-in? first-out?)))



(comment
  (-> (meta cached-hyp)
      :cache
      (search [3 4])))

(deftest basic-test
  (testing "Test caching facilities"
    (is (cached? cached-hyp 3 4))
    (is (respects-limit? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))
    (is (fifo? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))))
