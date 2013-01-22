(defproject paracelsus "0.0.1-SNAPSHOT"
  :description "A Clojure to BrightScript transpiler"
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :main ^{:skip-aot true} paracelsus.core
  :repl-init paracelsus.core)
