(ns kosmos.orientdb.server
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component])
  (:import com.orientechnologies.orient.server.OServerMain
           com.orientechnologies.orient.server.OServer
           com.orientechnologies.orient.core.Orient))

(defn shut-down-hook [component]
  (fn []
    (log/info "Shutdown hook called on OrientDB")
    (component/stop component)))

(defrecord OrientServer []

  component/Lifecycle
  (start
   [component]
   (log/info "OrientDB Server Startup ...")
   (let [configuration  (io/input-stream (io/resource "orient-db.config"))

         server         (doto (OServerMain/create)
                          (.startup configuration)
                          (.activate))

        #_#_ _              (doto (Orient/instance)
                          (.removeSignalHandler))

         component      (assoc component :server server :shutdown (atom nil))

         shutdown       (Thread. #(shut-down-hook component))]

     (log/info "Adding shutdown hook for OrientDB")
     (reset! (:shutdown component) shutdown)
     (.addShutdownHook (Runtime/getRuntime) shutdown)

     (log/info "OrientDB Server started successfully")
     component))

  (stop
   [{:keys [server shutdown] :as component}]
   (log/info "OrientDB Server Shutdown")
   (if server
     (let [_ (.shutdown server)]
       (log/info "OrientDB Server stopped successfully"))
     (log/info "NO OrientDB Server found in component ... unable to shutdown successfully"))
   (.removeShutdownHook (Runtime/getRuntime) @shutdown)
   (dissoc component :server :shutdown)))
