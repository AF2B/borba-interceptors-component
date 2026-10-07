(ns borba.interceptors.component
  "The Integrant component that builds every registered interceptor into one
   map, from the keyword it is registered under to the interceptor:

     :service/interceptors {:components #ig/ref :service/components}

   Routes and handlers take that map as `:interceptors` and resolve the
   keywords of their chains in it. The interceptors are found by the
   `defmethod`s of borba.interceptors.registry/interceptor, so the namespaces
   that register them must be loaded before the system starts: list them under
   `:service/namespaces` of the configuration."
  (:require
   [borba.interceptors.registry :as registry]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [integrant.core :as ig]))

(def ^:private stages [:enter :leave :error])

(defn- callable?
  "Returns true for a function, or a var that holds one, as a stage."
  [stage]
  (or (fn? stage) (var? stage)))

(defn- problem
  "Returns what keeps an interceptor from running in a chain, as data, or nil
   when it can run."
  [interceptor]
  (cond
    (not (map? interceptor))
    {:reason :not-a-map}

    (not-any? #(contains? interceptor %) stages)
    {:reason :no-stage}

    :else
    (when-let [stage (->> stages
                          (filter #(contains? interceptor %))
                          (remove #(callable? (get interceptor %)))
                          first)]
      {:reason :stage-not-callable
       :stage  stage})))

(defn- build-one
  "Builds the interceptor registered under a key, and fails when it cannot run
   in a chain."
  [components
   interceptor-key]
  (let [interceptor (registry/interceptor interceptor-key components)]
    (when-let [found (problem interceptor)]
      (throw (ex-info (str "the interceptor " interceptor-key " is not valid: "
                           (name (:reason found)))
                      (assoc found
                             :error       ::invalid-interceptor
                             :interceptor interceptor-key))))
    (assoc interceptor :name interceptor-key)))

(defn build
  "Builds every registered interceptor and returns them in a map from the
   keyword each is registered under to the interceptor, named by that keyword.
   Fails naming the keyword of the first interceptor that cannot run in a chain,
   which is a map with none of :enter, :leave and :error, or with one that is
   not a function.
   - components: the components of the service, handed to each interceptor
     when it is built"
  [components]
  (let [registered (dissoc (methods registry/interceptor) :default)]
    (into {}
          (map (fn [interceptor-key]
                 [interceptor-key (build-one components interceptor-key)]))
          (keys registered))))

(defmethod ig/init-key :service/interceptors
  [_ {:keys [components]}]
  (let [interceptors (build components)]
    (if (empty? interceptors)
      (log/info "registered no interceptors")
      (log/infof "registered %d interceptor(s): %s"
                 (count interceptors)
                 (str/join ", " (sort (map str (keys interceptors))))))
    interceptors))
