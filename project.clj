(defproject kosmos/kosmos-orientdb-server "0.0.2-SNAPSHOT"

  :description "orientdb server component"

  :url "https://bitbucket.org/pupcus/kosmos-orientdb-server"

  :scm {:url "git@bitbucket.org:bitbucket/kosmos-orientdb-server"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[kosmos "0.0.6"]
                 [org.clojure/tools.logging "0.3.1"]

                 [com.orientechnologies/orientdb-core "2.2.16"]
                 [com.orientechnologies/orientdb-server "2.2.16"]
                 [com.orientechnologies/orientdb-distributed "2.2.16"]]

  :profiles {:dev {:resource-paths ["dev-resources"]
                   :dependencies   [[org.clojure/clojure "1.8.0"]
                                    [hiccup "1.0.5"]
                                    [org.clojure/java.jdbc "0.6.1"]
                                    [org.slf4j/slf4j-log4j12 "1.7.5"]]}}

  :deploy-repositories [["snapshots"
                         {:url "https://clojars.org/repo"
                          :creds :gpg}]
                        ["releases"
                         {:url "https://clojars.org/repo"
                          :creds :gpg}]]

  :global-vars {*warn-on-reflection* true
                *assert* false})
