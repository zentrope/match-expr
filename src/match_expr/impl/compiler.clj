(ns match-expr.impl.compiler
  (:refer-clojure :exclude [eval compile comp])
  (:require
    [match-expr.impl.cidr :as cidr]
    [clojure.edn :as reader]
    [clojure.test :refer :all]))

;;-----------------------------------------------------------------------------

(defn- error
  [& terms]
  (throw (ex-info (clojure.string/join " " terms)) {}))

(defn- lookup
  [data a]
  (get data a))

(defn- truthy
  [v]
  (and (not (empty? v))
       (not (nil? v))))

(defn- to-int
  [s]
  (try
    (if (integer? s)
      s
      (Long/parseLong (if (coll? s) (first s) s)))
    (catch Throwable t
      (error "Unable to parse as number:" s))))

;;-----------------------------------------------------------------------------
;; Match ops (strings)

(defn- comparison-op
  [op]
  (fn [data k v]
    (op (to-int (lookup data k))
        (to-int v))))

(defn- op-cidr
  [data k v]
  (let [val (lookup data k)]
    (cond
      (nil? val) false
      (coll? val) (truthy (filter #(cidr/in-range? % v) val))
      :else (cidr/in-range? (lookup data k) v))))

(defn- op-match
  [data k v]
  (let [val (lookup data k)
        pat (re-pattern v)]
    (cond
      (nil? val) false
      (coll? val) (truthy (filter #(re-find pat %) val))
      :else  (truthy (re-find pat (lookup data k))))))

(defn- op-equals
  [data k v]
  (let [val (get data k)]
    (cond
      (nil? val) false
      (coll? val) (contains? (set val) v)
      :else (= val v))))

(defn- op-not-equals
  [data k v]
  (not (op-equals data k v)))

(defn- lookup-op
  [op]
  (case op
    =     op-equals
    not=  op-not-equals
    cidr  op-cidr
    match op-match
    >     (comparison-op >)
    <     (comparison-op <)
    <=    (comparison-op <=)
    >=    (comparison-op >=)
    (error "Unknown operator:" op)))

;;-----------------------------------------------------------------------------

(declare comp)

(defn- comp-and
  [exprs]
  (let [fns (doall (map comp exprs))]
    (fn [data]
      (every? #(% data) fns))))

(defn- comp-or
  [exprs]
  (let [fns (doall (map comp exprs))]
    (fn [data]
      (not (nil? (some #(% data) fns))))))

(defn- comp-matching-clause
  [[op & exprs]]
  (let [op-fn (lookup-op op)]
    (fn [data]
      (op-fn data (first exprs) (second exprs)))))

(defn- comp
  [expr]
  (cond
    (= (first expr) 'and) (comp-and (rest expr))
    (= (first expr) 'or) (comp-or (rest expr))
    :else (comp-matching-clause expr)))

;;-----------------------------------------------------------------------------

(defn compile
  "Return the compiled version of expr."
  [expr]
  (cond
    (fn? expr) expr
    (string? expr) (comp (reader/read-string expr))
    :else (comp expr)))

(defn eval
  "Evaluate expr in terms of the data (a k/v map). If expr
   is not compiled, it's compiled first."
  [data expr]
  ((compile expr) data))
