# bb-tower-deploy

A babashka library for deploying babashka projects to Tower.

## Usage

### Option 1: Install with bbin

Install:

```bash
bbin install https://github.com/socksy/bb-tower-deploy
```

Then run in any babashka project:

```bash
tower-setup
# or with options:
tower-setup --app-name "my-app" --default-task "sync" --babashka-version "1.3.191"
```

### Option 2: Add to your project's bb.edn

Add as a git dependency in your `bb.edn`:

```clojure
{:tasks {setup-tower {:requires ([bb-tower-deploy.core])
                      :task (bb-tower-deploy.core/setup {:app-name "linear-todoist-sync"
                                                         :default-task "sync"})
                      :extra-deps {io.github.socksy/bb-tower-deploy {:git/tag "v1" :git/sha "70da9ee"}}}}}
```

Then run:

```bash
bb setup-tower
```

## What it does

This will:
1. Create a `Towerfile` configured for babashka projects
2. Create a `bb_wrapper.py` Python script that runs your babashka tasks
3. Download babashka binaries for Linux (AMD64 and ARM64, although only AMD64 runners are supported on Tower atm)
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
uvx tower deploy
```

You can (and should!) set the `bb_task` parameter in Tower to specify which babashka task to run.
