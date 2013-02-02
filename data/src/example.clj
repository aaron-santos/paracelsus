(defn set-theme []
  (when-let [app    (create-object "roAppManager")]
    (when-let [theme  (create-object "roAssociativeArray")]
      (.SetTheme app theme))))

(defn -main []
  (set-theme))
