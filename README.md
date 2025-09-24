# bb-tower-deploy

A tool for deploying your babashka script as a [Tower](https://tower.dev/) app. Tower is meant for Python and data engineers, but why should I let that stop me? Why? Why not? (But also, because I want to schedule a script to run on a schedule without working out how to do that on a VPS or Raspberry Pi...)

Basically, we shim the babashka script by downloading the statically linked binary, and creating a Python script wrapper around it that simply calls it with the arguments provided (passed in via the tower `[[parameters]]`). Then, you can deploy the app and it will upload all of that in a bundle, none the wiser that you actually passed it Clojure code 😈.

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
                      :extra-deps {io.github.socksy/bb-tower-deploy {:git/tag "v1" :git/sha "f737fa7"}}}}}
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

## Limitations

### No deps
Because the Tower runners don't have java installed, you can't put anything in `:deps`, since [babashka.deps](https://book.babashka.org/#babashkadeps) has java as a prerequisite :( Theoretically, perhaps tools-deps-native could solve this (@borkdude linked me [this](https://github.com/babashka/pod-registry/blob/master/examples/tools-deps-native.clj) example script using a native build as a pod) but I haven't tried it (and it would necessarily be kinda hacky. For tasks that you don't need to run on the tower production, it's best to use `:extra-deps` on the task itself (see the example of how to add the setup-tower task of this repo to your bb.edn!).

### Long upload times
Since we're shipping the entire statically linked babashka, which is a graalvm image so presumably has the entire JVM inside of it, we end up having quite a large bundle we need to deploy to Tower. Which at least for me, takes a long time --- I'm presuming because it's being encrypted on the fly. Or perhaps my internet just sucks? Either way, one way to reduce the bundle size would be to only upload the amd64 babashka binary. For now, it's the only type of runner available anyway. You can specify exactly what you want to upload to `source` in the Towerfile (see the [Towerfile docs](https://docs.tower.dev/docs/reference/towerfile)). You'll need at least the following:

```
source = [
    "bb.edn",
    "bin/bb-linux-amd64",
    "bb_wrapper.py"
    "your_babashka_script.bb"
    # also possible:
    # src/**/*.clj
]
```
