(ns match-expr.core
  (:refer-clojure :exclude [eval compile])
  (:require
    [match-expr.impl.compiler :as compiler]))

(defn parse
  [expr]
  (compiler/compile expr))

(defn compile
  [expr]
  (parse expr))

(defn eval
  [data expr]
  (compiler/eval data expr))

(defn -main
  [& args]
  (println "Hello, World!"))
