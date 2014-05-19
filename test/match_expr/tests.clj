(ns match-expr.tests
  (:refer-clojure :exclude [eval])
  (:require
    [clojure.test :refer :all]
    [match-expr.core :refer [parse eval]]))

(defn do-eval
  [data expr]
  (eval data (parse (str expr))))

;; |----------+----------------------------+-----------------------------|
;; | Operator | Function                   | Example                     |
;; |----------+----------------------------+-----------------------------|
;; | =        | exact match                | (= :p "192.168.1.10")       |
;; | not=     | does not match             | (not= :p "foo")             |
;; | cidr     | match on network CIDR      | (cidr :p "192.168/16")      |
;; | match    | regular expression match   | (match :p "^.*host.com")    |
;; |----------+----------------------------+-----------------------------|

(deftest test-equality
  (testing "Testing = operator."
    (let [data {:a "1" :b #{"2" "3"}}]
      (is (true?  (do-eval data '(= :a "1"))))
      (is (true?  (do-eval data '(= :b "2"))))
      (is (true?  (do-eval data '(= :b "3"))))
      (is (false? (do-eval data '(= :a "2")))))))

(deftest test-not-equality
  (testing "Testing not= operator"
    (let [data {:a "1" :b #{"2" "3"}}]
      (is (false? (eval data '(not= :a "1"))))
      (is (false? (eval data '(not= :b "2"))))
      (is (false? (eval data '(not= :b "3"))))
      (is (true?  (eval data '(not= :a "2")))))))

(deftest test-cidr
  (testing "Testing cidr operator"
    (let [data {:a "10.10.10.10" :b #{"192.168.1.3" "192.168.2.3"}}]
      (is (true?  (do-eval data '(cidr :a "10.10/16"))))
      (is (false? (do-eval data '(cidr :a "10.9/16"))))
      (is (true?  (do-eval data '(cidr :b "192.168.1/24"))))
      (is (true?  (do-eval data '(cidr :b "192.168.2/24"))))
      (is (false? (do-eval data '(cidr :b "192.168.3/24")))))))

(deftest test-match
  (testing "Match operator"
    (let [data {:a #{"Windows 2008 R2" "Something Else R2"}
                :b #{"Ubuntu Linux" "An R2 Linux" "Debian Linux"}}]
      (is (true?  (do-eval data '(match :a "^[!W].*R2$"))))
      (is (false? (do-eval data '(match :b "^[!W].*R2$"))))
      (is (true?  (do-eval data '(match :a "Windows"))))
      (is (false? (do-eval data '(match :a "Linux"))))
      (is (false? (do-eval data '(match :b "Windows"))))
      (is (true?  (do-eval data '(match :b "Linux")))))))

;; |----------+----------------------------+-----------------------------|
;; | Operator | Function                   | Example                     |
;; |----------+----------------------------+-----------------------------|
;; | >        | greater than               | (> :p 23)                   |
;; | <        | less than                  | (< :p 33)                   |
;; | <=       | less than or equal to      | (<= :p 33)                  |
;; | >=       | greather than or equal to  | (>= :p 44)                  |
;; |----------+----------------------------+-----------------------------|

(deftest test-comparison-operators
  (testing "Comparison operators."
    (is (true?  (do-eval {:a "1"} '(>= :a "1"))))
    (is (true?  (do-eval {:b "2"} '(<= :b "2"))))
    (is (true?  (do-eval {:b "2"} '(< :b 3))))
    (is (true?  (do-eval {:a "1"} '(> :a 0))))
    (is (false? (do-eval {:a "1"} '(<= :a "0"))))
    (is (false? (do-eval {:b "2"} '(>= :b "3"))))
    (is (false? (do-eval {:b "2"} '(> :b 3))))
    (is (false? (do-eval {:a "1"} '(< :a 0))))))

;; TODO: what to do when num-rel encounters multiple vals?

;; |----------+----------------------------+-----------------------------|
;; | Operator | Function                   | Example                     |
;; |----------+----------------------------+-----------------------------|
;; | and      | less than or equal to      | (and (<= :p 33) (>= :p 44)) |
;; | or       | true of any clause is true | (or (>= :p 100) (<= :p 50)) |
;; |----------+----------------------------+-----------------------------|

(deftest test-logical-and
  (testing "Logical and"
    (let [data {:a "1" :b #{"c" "d"} :c #{"d" "e"}}]
      (is (true?  (do-eval data '(and (= :a "1") (= :b "c")))))
      (is (true?  (do-eval data '(and (= :c "d") (= :c "e")))))
      (is (false? (do-eval data '(and (= :a "1") (= :b "x"))))))))

(deftest test-logical-or
  (testing "Logical or"
    (let [data {:a "1" :b #{"c" "d"} :c #{"d" "e"}}]
      (is (true?  (do-eval data '(or (= :a "1") (= :a "x")))))
      (is (true?  (do-eval data '(or (= :a "x") (= :a "1")))))
      (is (false? (do-eval data '(or (= :a "x") (= :b "y") (= :c "z"))))))))

;; Esoteric cases

(deftest test-attribute-not-present
  (testing "That a non-present attribute returns false for the whole expr."
    ;;
    ;; If the attribute isn't present, assume the match is no good.
    ;;
    (is (false? (do-eval {:a "1" :b "2"} '(= :c "3"))))
    ;;
    ;; However, an "or" is okay because the "false" match clause is
    ;; just false and "or" is specifically designed for this case.
    ;;
    (is (true?  (do-eval {:a "1" :b "2"} '(or (= :c "3") (= :a "1")))))))
