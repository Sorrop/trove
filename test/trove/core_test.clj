(ns trove.core-test
  (:require [clojure.test :refer :all]
            [trove.core :refer :all]))

(defn inspect [c-fun]
  (-> (meta c-fun)
      :cache
      (fetch)))

(defn size-of-cache [c-fun]
  (-> (inspect c-fun)
      deref
      count))

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

(defn respects-limit?
  [fun limit input excess]
  (let [c-fun (cached-fn fun limit)]
    (doseq [args input]
      (apply c-fun args))
    (apply c-fun excess)
    (= 1 (size-of-cache c-fun))))

(comment
  (-> (meta cached-hyp)
      :cache
      (search [3 4])))

(deftest basic-test
  (testing "Test caching facilities"
    (is (cached? cached-hyp 3 4))
    (is (respects-limit? hypotenuse 3 [[3 4] [5 12] [6 8]] [21 28]))))
