(ns match-expr.core
  (:refer-clojure :exclude [eval])
  (:gen-class)
  (:require
    [match-expr.interpreter :as interpreter]))

(defn parse
  [expr]
  (if (string? expr)
    (interpreter/parse expr)
    expr))

(defn eval
  [data expr]
  (interpreter/eval data expr))

(defn -main
  [& args]
  (println "Hello, World!"))
