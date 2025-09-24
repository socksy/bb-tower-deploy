(ns bb-tower-deploy.core
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [babashka.http-client :as http]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn render-template [template-content substitutions]
  "Render a template with given substitutions"
  (reduce (fn [content [placeholder value]]
            (str/replace content placeholder value))
          template-content
          substitutions))

(defn create-towerfile [{:keys [app-name default-task]}]
  "Creates a Towerfile with some sensible defaults for babashka projects"
  (when-not (fs/exists? "Towerfile")
    (println "Creating Towerfile...")
    (spit "Towerfile"
          (render-template (slurp (io/resource "Towerfile"))
                          {"{{APP_NAME}}" (or app-name "babashka-app")
                           "{{DEFAULT_TASK}}" (or default-task "main")}))
    (println "Towerfile created")))

(defn create-python-wrapper []
  "Creates the Python wrapper script that calls babashka"
  (when-not (fs/exists? "bb_wrapper.py")
    (println "Creating Python wrapper (bb_wrapper.py)...")
    (spit "bb_wrapper.py" (slurp (io/resource "bb_wrapper.py")))
    (shell "chmod +x bb_wrapper.py")
    (println "bb_wrapper.py created")))

(defn fetch-babashka-binaries [version]
  "Downloads babashka binaries for Linux AMD64 and ARM64"
  (println (str "Fetching babashka binaries (version: " version ") for Linux deployment..."))
  (fs/create-dirs "bin")

  (let [actual-version (if (= version "latest")
                         (str/trim (:body (http/get "https://raw.githubusercontent.com/babashka/babashka/refs/heads/master/resources/BABASHKA_RELEASED_VERSION")))
                         version)]
    (println "Downloading AMD64 binary...")
    (with-open [stream (:body (http/get (str "https://github.com/babashka/babashka/releases/download/v" actual-version "/babashka-" actual-version "-linux-amd64-static.tar.gz") {:as :stream}))]
      (shell {:in stream :dir "bin"} "tar -xz bb"))
    (fs/move "bin/bb" "bin/bb-linux-amd64")

    (println "Downloading ARM64 binary...")
    (with-open [stream (:body (http/get (str "https://github.com/babashka/babashka/releases/download/v" actual-version "/babashka-" actual-version "-linux-aarch64-static.tar.gz") {:as :stream}))]
      (shell {:in stream :dir "bin"} "tar -xz bb"))
    (fs/move "bin/bb" "bin/bb-linux-aarch64"))

  (println "Babashka binaries downloaded successfully"))

(defn update-gitignore []
  "Adds babashka binaries to .gitignore"
  (when (fs/exists? ".gitignore")
    (let [current-content (slurp ".gitignore")
          entries ["bin/bb-linux-amd64" "bin/bb-linux-aarch64"]
          missing-entries (filter #(not (str/includes? current-content %)) entries)]
      (when (seq missing-entries)
        (println "Updating .gitignore...")
        (spit ".gitignore"
              (str current-content
                   (when-not (str/ends-with? current-content "\n") "\n")
                   (str/join "\n" missing-entries) "\n"))
        (println ".gitignore updated")))))

(defn setup [{:keys [babashka-version] :as opts}]
  "Sets up everything needed for Tower deployment"
  (println "Setting up Tower deployment for babashka project...")
  (create-towerfile opts)
  (create-python-wrapper)
  (fetch-babashka-binaries (or babashka-version "latest"))
  (update-gitignore)
  (println "\nTower deployment setup complete!")
  (println "\nNext steps:")
  (println "1. Customize your Towerfile as needed")
  (println "2. Set bb_task parameter in Tower to specify which task to run")
  (println "3. Deploy with: tower deploy"))

(defn -main [& args]
  "Main entry point for bbin"
  (let [opts (apply hash-map args)]
    (setup {:app-name (get opts "--app-name")
            :default-task (get opts "--default-task")
            :babashka-version (get opts "--babashka-version")})))
