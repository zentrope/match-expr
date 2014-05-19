(defproject match-expr "0.1.0"

  :description "Lisp-like expressions for matching hashmaps."

  :url "https://github.com/zentrope/match-expr"

  :license {:name "EPL" :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main ^:skip-aot match-expr.core
  :min-lein-version "2.3.4"
  :target-path "target/%s"
  :jvm-opts ["-Dapple.awt.UIElement=true"]
  :profiles {:uberjar {:aot :all}})
