(defproject kosmos/kosmos-orientdb-server "0.0.4-SNAPSHOT"

  :description "orientdb server component"

  :url "https://bitbucket.org/pupcus/kosmos-orientdb-server"

  :scm {:url "git@bitbucket.org:bitbucket/kosmos-orientdb-server"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[kosmos "0.0.7"]
                 [org.clojure/java.classpath "0.2.3"]
                 [org.clojure/tools.logging "0.4.0"]

                 [com.orientechnologies/orientdb-core "2.2.29"]
                 [com.orientechnologies/orientdb-server "2.2.29"]
                 [com.orientechnologies/orientdb-distributed "2.2.29"]]

  :profiles {:dev {:resource-paths ["dev-resources"]
                   :dependencies   [[org.clojure/clojure "1.8.0"]
                                    [hiccup "1.0.5"]
                                    [org.clojure/java.jdbc "0.7.3"]
                                    [org.slf4j/slf4j-log4j12 "1.7.25"]]}}

  :deploy-repositories [["snapshots"
                         {:url "https://clojars.org/repo"
                          :sign-releases false
                          :creds :gpg}]
                        ["releases"
                         {:url "https://clojars.org/repo"
                          :sign-releases false
                          :creds :gpg}]]

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :global-vars {*warn-on-reflection* true
                *assert* false})
