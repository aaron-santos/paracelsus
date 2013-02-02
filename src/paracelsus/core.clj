(ns paracelsus.core
  (:refer-clojure :exclude [-> ->> .. amap and areduce alength aclone assert binding bound-fn case comment cond condp
                            declare definline definterface defmethod defmulti defn defn- defonce
                 
                            with-loading-context with-local-vars with-open with-out-str with-precision with-redefs
                            satisfies? indentical? true? false? nil? str get

                            aget aset
                            + - * / < <= > >= == zero? pos? neg? inc dec max min mod
                            bit-and bit-and-not bit-clear bit-flip bit-not bit-or bit-set
                            bit-test bit-shift-left bit-shift-right bit-xor])
  (:require clojure.walk)
  (:gen-class :main true))

(alias 'core 'clojure.core)

;; Safe macros decompose into safe constructs
(core/defn strip-ns [form]
    (clojure.walk/postwalk-replace
      '{;clojure.core/if-let   if-let
        ;clojure.core/when-let when-let
        clojure.core/when     'when-safe}
      form))

(core/defn safe-macro? [sym]
  (core/true?
    (some (partial = sym)
      #{'when-let
        'if-let})))

(core/defn macroexpand-some [pred? form]
  (clojure.walk/prewalk
    (core/fn [x]
      (if (core/and (seq? x)
                    (pred? (first x)))
            (macroexpand x)
            x))
    form))

(core/defn sanitize-fn-name [fn-name]
  (clojure.string/join
    (core/map clojure.string/capitalize
      (clojure.string/split (core/str fn-name) #"-"))))

(core/defn indent-str [level]
  (clojure.string/join (repeat (core/* level 4) " ")))

(core/declare transpile-form)

(core/defn format-literal [literal]
  (core/condp = (type literal)
    java.lang.String (format "\"%s\"" literal)
    clojure.lang.PersistentArrayMap    (format "{...}")
    clojure.lang.PersistentList$EmptyList    (format "[]")
    clojure.lang.PersistentList   (format "[...]")
    nil "invalid"
    (core/str literal)))

(core/defn defn-tp [form indent]
  (core/let [fn-name    (sanitize-fn-name (second form))
        args       (map core/str (nth form 2))
        body-exprs (nthrest form 3)]
    (println (format "%sFunction %s(%s)" (indent-str indent) fn-name (clojure.string/join "," args)))
    (core/doseq [expr body-exprs] (transpile-form expr (core/inc indent)))
    (println "End Function")))

(core/defn let-tp [form indent]
  (do
    (core/doseq [binding (partition 2 (second form))]
      (core/let [var-name (first binding)]
        (print (format "%s%s = " (indent-str indent) var-name))
        (transpile-form (last binding) 0)))
        (println "")
    (core/doseq [expr (nthrest form 2)] (transpile-form expr indent))))

(core/defn if-tp [form indent]
  (core/let [test     (second form)
             true-fn  (nth form 2)
             false-fn (nth form 3)]
    (println (format "%sIf %s Then" (indent-str indent) (transpile-form test)))
    (transpile-form true-fn (core/inc indent))
    (println (format "%sElse" (indent-str indent)))
    (transpile-form false-fn (core/inc indent))
    (println (format "%sEnd If" (indent-str indent)))))

(core/defn when-tp [form indent]
  (core/let [test     (second form)
             true-fn  (nth form 2)]
    (print (format "%sIf " (indent-str indent)))
    (transpile-form test)
    (println " Then")
    (transpile-form true-fn (core/inc indent))
    (println (format "%sEnd If" (indent-str indent)))))

(core/defn do-tp [form indent]
  (core/doseq [expr (rest form)]
    (transpile-form expr indent)))

(core/defn !=-tp [form indent]
  (do
    (transpile-form (first form) indent)
    (print " != ")
    (transpile-form (second form) indent)))

(core/defn fn-call [form indent]
  (core/let [starts-with-dot? (= (first (core/str (first form))) \.)]
    (if starts-with-dot?
      ; (.XXX object args..) fn call syntax
      (core/let [fn-name  (sanitize-fn-name (first form))
            obj-name (second form)
            args     (nthrest form 2)]
        (println (format "%s%s%s(%s)" (indent-str indent) obj-name fn-name (clojure.string/join "," (map format-literal args)))))
      ; (fn-name args..)
      (core/let [fn-name (sanitize-fn-name (first form))
            args    (rest form)]
        (println (format "%s%s(%s)" (indent-str indent) fn-name (clojure.string/join "," (map format-literal args))))))))

(core/defn transpile-form
  ([form] (transpile-form form 0))
  ([form indent]
    (if (seq? form)

      (core/condp = (first form)
        'defn   (defn-tp form indent)
        'let    (let-tp form indent)
        'clojure.core/let    (let-tp form indent)
        'let*   (let-tp form indent)
        'if     (if-tp form indent)
        'when   (when-tp form indent)
        'clojure.core/when   (when-tp form indent)
        'do     (do-tp form indent)
        '!=     (!=-tp form indent)
        (fn-call form indent))
      (print (format-literal form)))))

(core/defn transpilePath [path]
  (core/with-open [r (core/-> (java.io.FileReader. path)
                    (java.io.PushbackReader.))]
      (core/loop [c (clojure.lang.LispReader/read r false nil true)]
        (if (core/and (not= c nil) (not= c [nil]))
          (core/let [expanded-form (macroexpand-some safe-macro? c)
                     ns-stripped-form (strip-ns expanded-form)]
            '(println core-form)
            (println expanded-form)
            (transpile-form expanded-form)
            (recur (clojure.lang.LispReader/read r false nil true)))))))

; (transpilePaths ["./data/src/example.clj"])

(core/defn transpilePaths [paths]
  (core/doseq [path paths] (transpilePath path)))

(core/defn -main
  [& args]
  (println "Transpiling...")
  (transpilePaths args)
  (println "Done"))
