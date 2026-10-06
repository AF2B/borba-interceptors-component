(ns borba.interceptors.registry
  "The registry of the interceptors of a service, one `defmethod` per
   interceptor, which `:service/interceptors` builds into a map.

     (defmethod registry/interceptor :log-request
       [_ _components]
       {:enter (fn [ctx]
                 (log/info \"request\" (get-in ctx [:request :uri]))
                 ctx)})

   The dispatch value is the keyword that routes and handlers refer to the
   interceptor by. The method returns a map with at least one stage:

     :enter  (fn [ctx] ctx)        runs on the way in, in the order of the chain
     :leave  (fn [ctx] ctx)        runs on the way out, in the reverse order
     :error  (fn [ctx error] ctx)  runs when an interceptor throws

   A stage can be a var, so that redefining its function at the REPL takes
   effect. The :name of the interceptor is its dispatch value, so the method
   does not give one.

   The method receives the components of the service, to close over what the
   interceptor needs:

     (defmethod registry/interceptor :rate-limit
       [_ {:keys [rate-limiter]}]
       {:enter (fn [ctx] (rate-limiter/check! rate-limiter ctx))})")

(defmulti interceptor
  "Builds the interceptor registered under a keyword, and returns it as a map
   with :enter, :leave and/or :error.
   - dispatch-key: the keyword that routes and handlers refer to it by
   - components: the components of the service, as `:service/interceptors`
     was given them"
  (fn [dispatch-key _components] dispatch-key))
