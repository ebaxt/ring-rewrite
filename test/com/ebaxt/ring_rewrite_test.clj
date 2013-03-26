(ns com.ebaxt.ring-rewrite-test
  (:use [ring.adapter.jetty]
        [com.ebaxt.ring-rewrite]))

(defn html []
  (slurp "resources/foo.html"))

(def rewrite-handler
  (wrap-rewrite (fn [req] {:status 200 :headers {} :body (html)}))
  [[:combine "js/jquery" "cdn.foo.bar"]
   [:combine "app.js" "cdn.foo.bar"]
   [:rewrite #"\w+" "example.com"]])

