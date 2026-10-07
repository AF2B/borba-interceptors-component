(ns borba.interceptors.component-test
  (:require
   [borba.interceptors.component :as component]
   [borba.interceptors.logging :as logging]
   [borba.interceptors.registry :as registry]
   [clojure.test :refer [deftest is testing]]
   [integrant.core :as ig])
  (:import
   (clojure.lang MultiFn)))

(set! *warn-on-reflection* true)

(defn- with-registered
  "Registers interceptors for the duration of a function, and removes them
   after it, so the tests do not see each other's registrations.
   - interceptors: a map from the dispatch key to a function of the components
     that returns the interceptor
   - f: a function of no arguments"
  [interceptors
   f]
  (doseq [[interceptor-key factory] interceptors]
    (.addMethod ^MultiFn registry/interceptor
                interceptor-key
                (fn [_ components] (factory components))))
  (try
    (f)
    (finally
      (doseq [interceptor-key (keys interceptors)]
        (remove-method registry/interceptor interceptor-key)))))

(defn- thrown-data
  "Returns the data of the exception a function throws, or nil when it does
   not throw one."
  [f]
  (try (f)
       nil
       (catch clojure.lang.ExceptionInfo e (ex-data e))))

(def ^:private enter-stage (fn [ctx] (assoc ctx :entered true)))
(def ^:private leave-stage (fn [ctx] (assoc ctx :left true)))
(def ^:private error-stage (fn [ctx _error] (assoc ctx :failed true)))

(deftest build-test
  (testing "builds each registered interceptor, named by its key"
    (with-registered
      {::log   (constantly {:enter enter-stage})
       ::audit (constantly {:leave leave-stage :error error-stage})}
      (fn []
        (let [built (component/build {})]
          (is (= #{::log ::audit} (set (keys built))))
          (is (= ::log (:name (::log built))))
          (is (identical? enter-stage (:enter (::log built))))
          (is (identical? leave-stage (:leave (::audit built))))
          (is (identical? error-stage (:error (::audit built))))))))

  (testing "the key is the name, whatever name the method gave"
    (with-registered
      {::log (constantly {:name :something-else :enter enter-stage})}
      (fn []
        (is (= ::log (:name (::log (component/build {}))))))))

  (testing "hands the components to the method, to close over"
    (with-registered
      {::flagged (fn [{:keys [flag]}]
                   {:enter (fn [ctx] (assoc ctx :flag flag))})}
      (fn []
        (let [enter (:enter (::flagged (component/build {:flag :on})))]
          (is (= {:flag :on} (enter {})))))))

  (testing "a stage can be a var"
    (with-registered
      {::by-var (constantly {:enter #'identity})}
      (fn []
        (is (= #'identity (:enter (::by-var (component/build {}))))))))

  (testing "builds nothing when nothing is registered"
    (is (= {} (component/build {}))))

  (testing "the default method is not an interceptor"
    (with-registered
      {:default (constantly {:enter enter-stage})}
      (fn []
        (is (= {} (component/build {})))))))

(deftest invalid-interceptor-test
  (testing "an interceptor must be a map"
    (with-registered
      {::not-a-map (constantly enter-stage)}
      (fn []
        (is (= {:error       ::component/invalid-interceptor
                :interceptor ::not-a-map
                :reason      :not-a-map}
               (thrown-data #(component/build {})))))))

  (testing "an interceptor needs a stage, which a misspelt one is not"
    (with-registered
      {::no-stage (constantly {:enters enter-stage})}
      (fn []
        (is (= {:error       ::component/invalid-interceptor
                :interceptor ::no-stage
                :reason      :no-stage}
               (thrown-data #(component/build {})))))))

  (testing "a stage must be a function, and says which one is not"
    (with-registered
      {::not-callable (constantly {:enter enter-stage :leave {:not :a-fn}})}
      (fn []
        (is (= {:error       ::component/invalid-interceptor
                :interceptor ::not-callable
                :reason      :stage-not-callable
                :stage       :leave}
               (thrown-data #(component/build {}))))))))

(deftest component-test
  (testing "initialises into the map of interceptors"
    (with-registered
      {::log (constantly {:enter enter-stage})}
      (fn []
        (let [system (ig/init {:service/interceptors {:components {}}})]
          (is (= [::log] (keys (:service/interceptors system))))
          (ig/halt! system)))))

  (testing "takes no components when none are given"
    (with-registered
      {::log (constantly {:enter enter-stage})}
      (fn []
        (is (= [::log]
               (keys (:service/interceptors
                      (ig/init {:service/interceptors {}}))))))))

  (testing "says which interceptors it registered, in order"
    (with-registered
      {::log   (constantly {:enter enter-stage})
       ::audit (constantly {:leave leave-stage})}
      (fn []
        (let [entries (logging/call-capturing
                       #(ig/init {:service/interceptors {}}))]
          (is (= [(str "registered 2 interceptor(s): "
                       ":borba.interceptors.component-test/audit, "
                       ":borba.interceptors.component-test/log")]
                 (logging/messages entries)))))))

  (testing "says so, without a list, when it registered none"
    (let [entries (logging/call-capturing
                   #(ig/init {:service/interceptors {}}))]
      (is (= ["registered no interceptors"]
             (logging/messages entries))))))
