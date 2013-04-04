(defproject ring-rewrite "0.1.0-SNAPSHOT"
  :description "Ring middleware for defining and applying rewrite rules"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.1.8"]
                 [com.novemberain/pantomime "1.7.0"]]
  :profiles {
             :dev {:plugins [[lein-ring "0.8.3"]
                             [leinjacker "0.4.1"]]
                   :dependencies [[ring-mock "0.1.3"]
                                  [ring/ring-jetty-adapter "1.1.8"]
                                  [enlive "1.0.1"]]
                   :ring {:handler com.ebaxt.ring-rewrite-test/test-app}}})
