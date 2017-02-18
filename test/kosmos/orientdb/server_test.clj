(ns kosmos.orientdb.server-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as jdbc]
            [hiccup.core :as hiccup]
            [kosmos :refer [start! stop! map->system system]]
            [kosmos.orientdb.server.studio :as studio]))



(def server-config [:orient-server
                    [:network
                     [:protocols
                      [:protocol
                       {:name "binary"
                        :implementation "com.orientechnologies.orient.server.network.protocol.binary.ONetworkProtocolBinary"}]]
                     [:listeners
                      [:listener
                       {:ip-address "127.0.0.1"
                        :port-range "2424-2430"
                        :protocol "binary"}]]]
                    [:users
                     [:user
                      {:name "root"
                       :password "ThisIs4_T3ST!"}]]
                    [:properties
                     [:entry
                      {:name "log.console.level"
                       :value "info"}]
                     [:entry
                      {:name "plugin.dynamic"
                       :value "false"}]]])


(defn config-to-xml [{:keys [data] :as component}]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
       (hiccup/html data)))

(def config
  {
   :config-xml
   {
    :kosmos/init config-to-xml
    :data server-config
    }

   :orient-server
   {
    :kosmos/init :kosmos.orientdb.server/OrientDbServer
    :kosmos/requires {:config :config-xml}
    }
   })

(defn setup-db-server [f]
  (with-redefs [studio/ensure-studio-zip-file (fn [_] true)]
    (start! (map->system config))
    (f)
    (stop!)))

(use-fixtures :once setup-db-server)

(deftest server-started
  (let [server (get-in system [:orient-server :server])]
    (is (.isActive server))))

