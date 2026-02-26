(ns bb-tower-deploy.core
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [babashka.http-client :as http]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn render-template [template-content substitutions]
  (reduce (fn [content [placeholder value]]
            (str/replace content placeholder value))
          template-content
          substitutions))

(defn fetch-latest-bb-version []
  (str/trim (:body (http/get "https://raw.githubusercontent.com/babashka/babashka/refs/heads/master/resources/BABASHKA_RELEASED_VERSION"))))

(defn create-towerfile [{:keys [app-name default-task default-task-args]}]
  (when-not (fs/exists? "Towerfile")
    (println "Creating Towerfile...")
    (spit "Towerfile"
          (render-template (slurp (io/resource "Towerfile"))
                          {"{{APP_NAME}}" (or app-name "babashka-app")
                           "{{DEFAULT_TASK}}" (or default-task "main")
                           "{{DEFAULT_TASK_ARGS}}" (or default-task-args "")}))
    (println "Towerfile created")))

(defn create-python-wrapper [bb-version]
  (when-not (fs/exists? "bb_wrapper.py")
    (println "Creating Python wrapper (bb_wrapper.py)...")
    (spit "bb_wrapper.py"
          (render-template (slurp (io/resource "bb_wrapper.py"))
                          {"{{BB_VERSION}}" bb-version}))
    (shell "chmod +x bb_wrapper.py")
    (println "bb_wrapper.py created")))

(defn setup [{:keys [babashka-version] :as opts}]
  (println "Setting up Tower deployment for babashka project...")
  (let [bb-version (or babashka-version (do (println "Fetching latest babashka version...")
                                            (fetch-latest-bb-version)))]
    (println (str "Using babashka v" bb-version))
    (create-towerfile opts)
    (create-python-wrapper bb-version))
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
