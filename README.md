# KDev

A CLI tool for composing together and running remotely defined docker-compose files a reproducible manner. This additionally integrates with the `kl` networking tool to allow easily enabling/disabling kl proxies.

## Installation

You can use the below script to automatically install the latest release

```bash
bash < <(curl -s https://raw.githubusercontent.com/kepler16/kdev/master/install.sh)
```

Or you can get the binaries directly from the GitHub releases page and put them on your PATH.

### Getting Started

Kdev works on configuration groups. Groups are defined simply as folders in your `~/.config/kdev` directory that contain a `config.edn` file. A configuration is a group of named remote service configurations and looks something like:

```clojure
;; ~/.config/kdev/example/config.edn
{:service-name {:url "org/repo"
                :ref "optional-ref"
                :sha "optional-sha"
                :subdir "optional/subdir"}}
```

The `url` is expected to be the partial URL of a github repository. Currently only Github is supported.

you can now run

```bash
kdev start example
```

And your service configuration will be downloaded and executed locally. A lockfile will be generated at `config.lock.edn` and will contain the git sha the config was resolved against at the time of running `start`. If the remote service configuration changes at some point you can pull these changes in by running

```bash
kdev update example
# or
kdev pull example --update
```

The remote service configurations will be pulled down and built inside of `~/.config/kdev/<group>/.kdev`

### Creating remote service configurations

A remote service configuration is an edn file placed at (by default) `.kdev/service.edn` within the remote repository. This file is a superset of the docker-compose format, where each top-level docker-compose key is namespaced with `:compose/`. This file can also contain additional fields consumed only by kdev. Here is an example configuration file:

```clojure
;; .kdev/service.edn
{:kdev/include ["config/.env"]

 :compose/networks {:kl {:external true}}

 :kl/routes {:example {:domain "example.k44.test"
                       :port 3000}}

 :compose/services
 {:example
  {:image "ghcr.io/example/example:{{SHA}}"
   :restart "unless-stopped"
   :deploy {:resources {:limits {:memory "512m"}
                        :reservations {:memory "512m"}}}
   :volumes ["config/.env:/app/.env"]
   :kl/routes [:example]
   :expose [3000]}}}
```
