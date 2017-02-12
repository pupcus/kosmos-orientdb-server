(ns kosmos.orientdb.server
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component])
  (:import com.orientechnologies.orient.server.OServerMain
           com.orientechnologies.orient.server.OServer
           com.orientechnologies.orient.core.Orient))

(def DEFAULT_CONFIG_FILE "orient-db-server-config.xml")

(defn current-directory []
  (.getAbsolutePath (io/as-file ".")))

(defn str->stream [s]
  (java.io.ByteArrayInputStream. (.getBytes ^String s)))

(defn is-resource? [r]
  (try
    (io/resource r)
    (catch Exception e)))

(defn is-file? [f]
  (try
    (let [file (io/as-file f)]
      (and (.exists file)
           (.isFile file)))
    (catch Exception e)))

(defn can-open? [t]
  (cond
    (is-resource? t) :resource
    (is-file?     t) :file))

(defn as-stream-dispatch-fn [t]
  (or (can-open? t) (class t)))

(defmulti as-stream #'as-stream-dispatch-fn)

(defmethod as-stream :resource [r]
  (io/input-stream (io/resource r)))

(defmethod as-stream :file [f]
  (io/input-stream (io/as-file f)))

(defmethod as-stream java.io.InputStream [is]
  is)

(defmethod as-stream java.lang.String [s]
  (str->stream s))

(defrecord OrientDbServer []

  component/Lifecycle
  (start
    [{:keys [config] :as component}]
    (log/info "OrientDB Server Startup ...")

    (System/setProperty "ORIENTDB_HOME" (current-directory))

    (let [configuration (or (as-stream config)
                            (as-stream DEFAULT_CONFIG_FILE))

          server         (doto (OServerMain/create)
                           (.startup (as-stream configuration))
                           (.activate))

          component      (assoc component :server server)]

      (log/info "OrientDB Server started successfully")
      component))

  (stop
    [{:keys [server] :as component}]
    (log/info "OrientDB Server Shutdown")
    (if server
      (let [_ (.shutdown server)]
        (log/info "OrientDB Server stopped successfully")
        (dissoc component :server))
      (log/info "NO OrientDB Server found in component ... unable to shutdown successfully!"))))
