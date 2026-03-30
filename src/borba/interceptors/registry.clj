(ns borba.interceptors.registry
  "Multimethod registry for Pedestal interceptors.

   ── Pattern ───────────────────────────────────────────────────────────────────

   Define interceptors in your service by implementing this multimethod:

     (defmethod interceptors/interceptor :log-request [_ _]
       {:enter log-request-enter})

     (defmethod interceptors/interceptor :require-content-type [_ _]
       {:enter require-content-type-enter})

   The dispatch value is the keyword used to reference the interceptor in routes.

   ── Return value ──────────────────────────────────────────────────────────────

   The defmethod must return a map with at least one of:
     :enter  — (fn [ctx] ctx)   runs before the handler (left → right)
     :leave  — (fn [ctx] ctx)   runs after the handler  (right → left)
     :error  — (fn [ctx ex] ctx) catches exceptions in the chain

   The :name key is injected automatically by the component using the dispatch key.

   ── If the interceptor needs config ──────────────────────────────────────────

   Config is passed via Integrant through :service/interceptors init map.
   Access it via the second arg (the full components map):

     (defmethod interceptors/interceptor :rate-limit [_ {:keys [max-rps]}]
       {:enter (fn [ctx] (check-rate! max-rps ctx))})")

(defmulti interceptor
  "Registry of all Pedestal interceptors.
   Dispatch key is the interceptor keyword (e.g. :log-request).
   Returns an interceptor map with :enter and/or :leave and/or :error."
  (fn [dispatch-key _components] dispatch-key))
