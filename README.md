# bb-tower-deploy

A babashka library for deploying babashka projects to Tower.

## Usage

Add as a git dependency in your `bb.edn`:

```clojure
{:deps {io.github.socksy/bb-tower-deploy {:git/url "https://github.com/socksy/bb-tower-deploy"
                                          :git/sha "latest-sha-here"}}
 :tasks {setup-tower {:doc "Setup Tower deployment"
                     :task (bb-tower-deploy.core/setup {})}}}
```

Then run:

```bash
bb setup-tower-wrapper
```

This will:
1. Create a `Towerfile` configured for babashka projects
2. Create a `bb_wrapper.py` Python script that runs your babashka tasks
3. Download babashka binaries for Linux (AMD64 and ARM64)
4. Update your `.gitignore` to exclude the binaries

## Configuration

You can pass options to customize the setup:

```clojure
{:task (bb-tower-deploy.core/setup
        {:app-name "my-app"
         :default-task "sync"
         :babashka-version "1.3.191"})}
```

## Deployment

After setup, deploy with:

```bash
tower deploy
```

Set the `bb_task` parameter in Tower to specify which babashka task to run.
