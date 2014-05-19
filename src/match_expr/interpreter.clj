(ns match-expr.interpreter
  (:refer-clojure :exclude [eval])
  (:require
    [match-expr.cidr :as cidr]
    [clojure.edn :as reader]))

;;----------------------------------------------------------------------------
;; Utils

(declare dsl-eval)

(defn- error!
  [env code msg & args]
  (let [err-msg (try (apply format msg (map str args)) (catch Throwable t ""))]
    (throw (ex-info err-msg {:code code :context env}))))

(defn- lookup-sym
  "Return the value for the given symbol in the environment's asset."
  [env sym]
  (if-let [value (sym env)]
    value
    (error! env :unknown-attr "Unknown attribute: [%s]." sym)))

(defn- to-int
  [s]
  ;;
  ;; Checks for a set due to the way we store values, as sets, even if
  ;; there's one. Numeric operators will always assume there's just
  ;; one value, so we take the first.
  ;;
  (try
    (if (integer? s)
      s
      (Long/parseLong (if (coll? s) (first s) s)))
    (catch Throwable t
      (error! {} :parse-error "Unable to parse '%s' as number." s))))

;;----------------------------------------------------------------------------
;; Operators

(defn- op-or
  "Evaluate each expression, returning true for the first true
  expression, or false if none are true."
  [env exprs]
  (loop [e exprs]
    (if (empty? e)
      false
      (if-let [v (dsl-eval env (first e))]
        true
        (recur (rest e))))))

(defn- op-and
  "Evaluate each expression, returning true if all the expressions
  evalute to true."
  [env exprs]
  (loop [e exprs]
    (if (empty? e)
      true
      (if-let [v (dsl-eval env (first e))]
        (recur (rest e))
        false))))

(defn- op-equals
  [env k v]
  (let [val (lookup-sym env k)]
    (if (set? val)
      (contains? val v)
      (= val v))))

(defn- op-not-equals
  [env k v]
  (not (op-equals env k v)))

(defn- gen-math-op
  [op]
  (fn [env k v]
    (op (to-int (lookup-sym env k))
        (to-int v))))

(defn- truthy
  [v]
  (and (not (empty? v))
       (not (nil? v))))

(defn- op-cidr
  [env k v]
  (let [val (lookup-sym env k)]
    (if (set? val)
      (truthy (filter #(cidr/in-range? % v) val))
      (cidr/in-range? (lookup-sym env k) v))))

(defn- op-match
  [env k v]
  (let [val (lookup-sym env k)
        pat (re-pattern v)]
    (if (set? val)
      (truthy (filter #(re-find pat %) val))
      (truthy (re-find pat (lookup-sym env k))))))

(defn- lookup-op
  [sym]
  (case sym
    = op-equals
    not= op-not-equals
    cidr op-cidr
    match op-match
    > (gen-math-op >)
    < (gen-math-op <)
    >= (gen-math-op >=)
    <= (gen-math-op <=)
    (error! {} :unknown-op "Unrecognized operator: [%]." sym)))

;;----------------------------------------------------------------------------

(defn- dsl-apply
  [env op args]
  (try
    (case op
      and (op-and env args)
      or (op-or env args)
      (apply (lookup-op op) env (map #(dsl-eval env %) args)))
    ;;
    ;; Treat an unknown attr error as a false result, but allow the
    ;; evaluator to continue.
    ;;
    (catch clojure.lang.ExceptionInfo t
      (if (= (:code (ex-data t)) :unknown-attr)
        false
        (throw t)))))

(defn- dsl-eval
  [env expr]
  (cond
   (keyword? expr) expr
   (string? expr) expr
   (number? expr) expr
   (coll? expr) (dsl-apply env (first expr) (rest expr))
   :else (error! env :not-parseable "Unable to parse: [%s]." expr)))

;;----------------------------------------------------------------------------
;; API

(defn eval
  "Evaluate the expression, returning true or false."
  [data expr]
  (try
    (dsl-eval data expr)
    (catch clojure.lang.ExceptionInfo t
      (throw t))))

(defn parse
  "Convert expression to EDN for evaluation."
  [expr]
  (if (string? expr)
    (reader/read-string expr)
    expr))
