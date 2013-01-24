(ns paracelsus.core
  (:refer-clojure :exclude [-> ->> .. amap and areduce alength aclone assert binding bound-fn case comment cond condp
                            declare definline definterface defmethod defmulti defn defn- defonce
                            defprotocol defrecord defstruct deftype delay destructure doseq dosync dotimes doto
                            extend-protocol extend-type fn for future gen-class gen-interface
                            if-let if-not import io! lazy-cat lazy-seq let letfn locking loop
                            memfn ns or proxy proxy-super pvalues refer-clojure reify sync time
                            when when-first when-let when-not while with-bindings with-in-str
                            with-loading-context with-local-vars with-open with-out-str with-precision with-redefs
                            satisfies? identical? true? false? nil? str get

                            aget aset
                            + - * / < <= > >= == zero? pos? neg? inc dec max min mod
                            bit-and bit-and-not bit-clear bit-flip bit-not bit-or bit-set
                            bit-test bit-shift-left bit-shift-right bit-xor])
  (:require clojure.walk))

(alias 'core 'clojure.core)

;; Import safe macros



(core/defn sanitize-fn-name [fn-name]
  (if (= (core/str fn-name) "-main")
         "Main"
          fn-name))

(core/declare  transpileForm)

(core/defn format-literal [literal]
  (core/condp = (type literal)
    java.lang.String (format "\"%s\"" literal)
    clojure.lang.PersistentArrayMap    (format "{...}")
    clojure.lang.PersistentList$EmptyList    (format "[]")
    clojure.lang.PersistentList   (format "[...]")
    (core/str literal)))

(core/defn defn-tp [form]
  (core/let [fn-name    (sanitize-fn-name (second form))
        args       (map core/str (nth form 2))
        body-exprs (nthrest form 3)]
    (println (format "Function %s(%s)" fn-name (clojure.string/join "," args)))
    (core/doseq [expr body-exprs] (transpileForm expr))
    (println "End Function")))

(core/defn let-tp [form]
  (do
    (core/doseq [binding (partition 2 (core/-> form rest butlast first))]
      (core/let [var-name (first binding)]
        (print var-name "= ")
        (transpileForm (last binding))))
    (core/doseq [expr (nthrest form 2)] (transpileForm expr))))

(core/defn fn-call [form]
  (core/let [starts-with-dot? (= (first (core/str (first form))) \.)]
    (if starts-with-dot?
      ; (.XXX object args..) fn call syntax
      (core/let [fn-name  (sanitize-fn-name (first form))
            obj-name (second form)
            args     (nthrest form 2)]
        (println (format "%s%s(%s)" obj-name fn-name (clojure.string/join "," (map format-literal args)))))
      ; (fn-name args..)
      (core/let [fn-name (sanitize-fn-name (first form))
            args    (rest form)]
        (println (format "%s(%s)" fn-name (clojure.string/join "," (map format-literal args))))))))

(core/defn transpileForm [form]
  (core/case (first form)
    defn (defn-tp form)
    let  (let-tp form)
    (fn-call form)))

(core/defn transpilePath [path]
  (core/with-open [r (core/-> (java.io.FileReader. path)
                    (java.io.PushbackReader.))]
      (core/loop [c (clojure.lang.LispReader/read r false nil true)]
        (if (core/and (not= c nil) (not= c [nil]))
          (do
            (transpileForm c)
            (recur (clojure.lang.LispReader/read r false nil true)))))))

; (transpilePaths ["./data/src/example.clj"])

(core/defn transpilePaths [paths]
  (core/doseq [path paths] (transpilePath path)))

(core/defn -main
  [& args]
  (println "Transpiling...")
  (transpilePaths args)
  (println "Done"))
