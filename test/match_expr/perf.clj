(ns match-expr.perf
  (:refer-clojure :exclude [eval compile])
  (:require
    [clojure.test :refer :all]
    [match-expr.impl.interpreter :as i]
    [match-expr.impl.compiler :as c]))


(def expr '(and (or (= :foo1 "bar")
                    (= :foo2 "bar"))
                (or (= :bar1 "foo")
                    (= :bar2 "foo"))))

(def i-expr (i/parse expr))
(def c-expr (c/compile expr))

(def data '{:foo1 "bar" :bar2 "foo"})

(defn runner
  [f d e]
  (let [s (System/currentTimeMillis)]
    (dotimes [i 10000]
      (f d e))
    (- (System/currentTimeMillis) s)))

(defn printer
  [module f]
  (let [timings (take-last 10 (for [i (range 50)] (f)))
        total (apply + timings)
        avg (double (/ total 10))]
    (println (format "%s: avg of last 10 of 50 runs: [%5s]." module avg))))

(deftest inter-perf
  (testing "Interpreter performance"
    (printer "interpreter " (fn [] (runner i/eval data i-expr)))))

(deftest comp-perf
  (testing "Compiled performance"
    (printer "compiler    " (fn [] (runner c/eval data c-expr)))))
