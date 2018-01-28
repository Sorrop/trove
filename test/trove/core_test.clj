(ns trove.core-test
  (:require [clojure.test :refer :all]
            [trove.core :refer :all]))

(defn fact [n]
  (if (= 1 n)
    1
    (*' n (fact (dec n)))))

(def cached-fact
  (cached-fn fact 100))

(comment
  (-> (meta cached-fact)
      :cache
      (fetch)
      deref))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
