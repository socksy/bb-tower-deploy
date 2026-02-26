(ns bb-tower-deploy.core
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [babashka.http-client :as http]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn render-template [template vars]
  (str/replace template #"\{\{(.+?)\}\}"
               (fn [[_ k]] (or (get vars (keyword k)) ""))))

(defn fetch-latest-bb-version []
  (str/trim (:body (http/get "https://raw.githubusercontent.com/babashka/babashka/refs/heads/master/resources/BABASHKA_RELEASED_VERSION"))))

(defn create-files [filenames vars]
  (doseq [filename filenames]
    (when-not (fs/exists? filename)
      (println (str "Creating " filename "..."))
      (spit filename (render-template (slurp (io/resource filename)) vars))
      (println (str filename " created")))))

(defn setup [{:keys [app-name default-task default-task-args babashka-version]}]
  (println "Setting up Tower deployment for babashka project...")
  (let [bb-version (or babashka-version (do (println "Fetching latest babashka version...")
                                            (fetch-latest-bb-version)))
        vars {:app-name (or app-name "babashka-app")
              :bb-version bb-version
              :default-task (or default-task "main")
              :default-task-args (or default-task-args "")}]
    (println (str "Using babashka v" bb-version))
    (create-files ["Towerfile" "bb_wrapper.py" "pyproject.toml" "pod_tower.py"] vars)
    (when (fs/exists? "bb_wrapper.py")
      (shell "chmod +x bb_wrapper.py")))
  (println "\nTower deployment setup complete!")
  (println "\nNext steps:")
  (println "1. Customize your Towerfile as needed")
  (println "2. Set bb_task parameter in Tower to specify which task to run")
  (println "3. Deploy with: tower deploy"))

(defn -main [& args]
  (let [opts (apply hash-map args)]
    (setup {:app-name (get opts "--app-name")
            :default-task (get opts "--default-task")
            :default-task-args (get opts "--default-task-args")
            :babashka-version (get opts "--babashka-version")})))
