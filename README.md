# trove

Experiment with different ways to have functions that store previous results (memoization) packaged with implementations of content replacement policies such as (FIFO,LIFO,LRU,MRU...).

## Usage

```
(ns my-example
  (:require [trove.core :as trove])

(defn my-function
  [a b]
  (+ a b))

(def my-function-cached
  ;; second argument is cache size
  (trove/fifo-cached-fn my-function 100))
```

Use my-function-cached as a regural function. Note that in order for all this to be valid, my-function should be referentially transparent i.e. produce the same output for the same input.

The project is in WIP state (and also, my pet).

## License

Copyright © 2018 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
