(ns paracelsus.core)

(defn transpilePath [path]
  nil)

(defn transpilePaths [paths]
  (doall (map transpilePath paths)))

(defn -main
  [& args]
  (println "Transpiling...")
  (transpilePaths args)
  (println "Done"))
