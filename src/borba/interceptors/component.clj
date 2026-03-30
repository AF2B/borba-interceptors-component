(ns borba.interceptors.component
  "Integrant component that auto-discovers all registered interceptors.

   ── How it works ──────────────────────────────────────────────────────────────

   On init, this component calls (methods interceptors/interceptor) to discover
   every defmethod registered under the multimethod. For each dispatch key it
   calls the defmethod and injects :name, producing a complete Pedestal
   interceptor map keyed by keyword.

   The resulting map is passed as :interceptors to :http/routes, which uses it
   to resolve plain keywords in route tuples:

     [\"/v1/users\" :post :user/create {:interceptors [:log-request :require-content-type]}]

   ── System EDN ────────────────────────────────────────────────────────────────

     :service/interceptors {:components #ig/ref :components/all}

   ── Interceptor resolution in routes ─────────────────────────────────────────

   borba-routes-component looks up each keyword in this map and inlines the
   full interceptor map into the Pedestal route interceptor chain."
  (:require [integrant.core :as ig]
            [borba.interceptors.registry :as registry]))

(defmethod ig/init-key :service/interceptors [_ {:keys [components]}]
  (let [dispatch-keys (-> (methods registry/interceptor) keys)]
    (reduce
     (fn [acc k]
       (let [interceptor-map (registry/interceptor k components)]
         (assoc acc k (assoc interceptor-map :name k))))
     {}
     dispatch-keys)))

(defmethod ig/halt-key! :service/interceptors [_ _] nil)
