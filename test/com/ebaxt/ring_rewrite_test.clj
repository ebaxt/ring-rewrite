(ns com.ebaxt.ring-rewrite-test
  (:use [ring.adapter.jetty]
        [com.ebaxt.ring-rewrite]
        [clojure.test]))

(defn html []
  (slurp "resources/foo.html"))

(def rewrite-handler
  (wrap-rewrite (fn [req] {:status 200 :headers {} :body (html)})
                [:rewrite "http://code.jquery.com" "http://cdn.com"]
                [:rewrite "js/" "http://cdn.com/"]
                [:rewrite #"css/(\w+)" "http://cdn.com/$1"]
                [:rewrite #"\".+/(img/\w+)" "\"http://mypics.com/$1"]))

(deftest it-works
  (let [response {:headers {}}
        handler (wrap-rewrite (fn [req] {:status 200 :headers {} :body (html)})
                              [:rewrite "js/bootstrap.min.js" "some.cdn.com" ])]
    (println (handler (constantly response)))))

