(defn set-theme []
  (let [app    (create-object "roAppManager")
        theme  (create-object "roAssociativeArray")]
      (.SetTheme app theme)))

(defn -main []
  (setTheme))
