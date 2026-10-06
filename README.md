# borba-interceptors-component

[![CI](https://github.com/AF2B/borba-interceptors-component/actions/workflows/ci.yml/badge.svg)](https://github.com/AF2B/borba-interceptors-component/actions/workflows/ci.yml)

A registry of [Pedestal](https://pedestal.io) interceptors for Borba services, and an [Integrant](https://github.com/weavejester/integrant)
component that builds every registered interceptor into one map. Routes and handlers refer to an interceptor by keyword and
look it up in that map, so a service declares an interceptor once and uses it wherever it is needed.

## Install

```clojure
io.github.af2b/borba-interceptors-component
{:git/url "https://github.com/AF2B/borba-interceptors-component"
 :git/tag "v1.1.0"
 :git/sha "<the commit of the tag, printed in the release notes>"}
```

It depends on Clojure, Integrant and `tools.logging`. It does not depend on Pedestal: an interceptor is a map, and the library
only checks that the map can run in a chain.

## Use

Register an interceptor with a `defmethod`. The dispatch value is the keyword the rest of the service refers to it by, and the
method returns a map with at least one stage: `:enter` runs on the way in, `:leave` on the way out, and `:error` when an
interceptor throws. The method receives the components of the service, to close over what the interceptor needs:

```clojure
(require '[borba.interceptors.component :as component]
         '[borba.interceptors.registry :as registry])

(defmethod registry/interceptor :log-request
  [_ _components]
  {:enter (fn [ctx]
            (println "request" (get-in ctx [:request :uri]))
            ctx)})

(defmethod registry/interceptor :rate-limit
  [_ {:keys [max-rps]}]
  {:enter (fn [ctx] (assoc ctx :max-rps max-rps))})
```

`build` turns everything that is registered into a map, from the keyword to the interceptor, named by that keyword:

```clojure
(def interceptors (component/build {:max-rps 100}))

(sort (keys interceptors))
;; => (:log-request :rate-limit)

(:name (:log-request interceptors))
;; => :log-request

((:enter (:rate-limit interceptors)) {})
;; => {:max-rps 100}
```

In a service the Integrant component does it, and hands it the components:

```clojure
{:service/namespaces [com.example.payments.interceptors]

 :ig/system
 {:service/interceptors {:components #ig/ref :service/components}}}
```

The interceptors are found by the `defmethod`s that have been loaded, so the namespaces that register them go under
`:service/namespaces`, which `borba-core-component` loads before the system starts. The component logs what it registered:

```
INFO: registered 2 interceptor(s): :log-request, :rate-limit
```

## What is checked

An interceptor that cannot run fails when the system starts, naming it, instead of failing on the first request:

```clojure
(defmethod registry/interceptor :broken [_ _] {:enters (fn [ctx] ctx)})

(component/build {})
;; throws ExceptionInfo "the interceptor :broken is not valid: no-stage"
;;   {:reason :no-stage
;;    :error :borba.interceptors.component/invalid-interceptor
;;    :interceptor :broken}
```

| `:reason` | When |
|---|---|
| `:not-a-map` | The method returned something other than a map |
| `:no-stage` | The map has none of `:enter`, `:leave` and `:error`, such as a misspelt one |
| `:stage-not-callable` | A stage is not a function or a var (`:stage` in the data says which) |

A stage can be a var, so that redefining its function at the REPL takes effect on the next request.

## API

| Name | What it does |
|---|---|
| `borba.interceptors.registry/interceptor` | The multimethod to register an interceptor with, dispatching on its keyword |
| `borba.interceptors.component/build` | Builds every registered interceptor into a map |
| `:service/interceptors` | The Integrant key that does it, with `:components` as its option |

## Design notes

- **The key is the name.** A `:name` in the map the method returns is replaced by the dispatch value, so the name in a
  Pedestal trace is the keyword the service uses everywhere else.
- **No Pedestal dependency.** The check is on the shape of the map, so the library works with whichever Pedestal the service
  uses.
- **The default method is not an interceptor.** A `:default` method, if a service defines one, is left out of the map.

## Development

```bash
make check      # lint, format, conventions, reflection, tests, coverage
make ci         # everything the pipelines enforce
```

See [CONTRIBUTING.md](CONTRIBUTING.md). The repository follows the [Borba standard](https://github.com/AF2B/borba-tooling/blob/main/docs/standard.md).

## License

[MIT](LICENSE)
