(ns com.ebaxt.ring-rewrite-test
  (:require [net.cgrand.enlive-html :as h])
  (:use [ring.adapter.jetty]
        [com.ebaxt.ring-rewrite]
        [clojure.test]
        [clojure.pprint]
        [ring.mock.request]))

(def html-page
  (slurp "resources/foo.html"))

(def rewrite-handler
  (fn [req]
    (pprint req)
    (rewrite-page req
                  [:rewrite #"css/(\w+)" "http://cdn.com/$1"]
                  [:rewrite "http://code.jquery.com" "http://cdn.com"]
                  [:rewrite #"http://(.+)/directory/(\w+)/(\w+)" "http://$1/$2/$3"]
                  [:rewrite #"\".+/(img/\w+)" "\"http://mypics.com/$1"])))

(defn- get-attr [nodes id t]
  (get-in (first (h/select nodes [(h/id= id)])) [:attrs t]))

(deftest rewrite-page-test
  (let [response {:headers {}}
        handler (rewrite-page (fn [req] {:status 200 :headers {} :body html-page})
                              [:rewrite #"css/(\w+)" "http://cdn.com/$1"]
                              [:rewrite "http://code.jquery.com" "http://cdn.com"]
                              [:rewrite #"http://(.+)/directory/(\w+)/(\w+)" "http://$1/$2/$3"]
                              [:rewrite #"\".+/(img/\w+)" "\"http://mypics.com/$1"])
        body (:body (handler (constantly response)))
        nodes (h/html-resource (java.io.StringReader. body))]
    (is (= "http://cdn.com/bootstrap.min.css" (get-attr nodes "a" :href)))
    (is (= "http://cdn.com/jquery.js" (get-attr nodes "b" :src)))
    (is (= "http://www.example.com/people/joe" (get-attr nodes "c" :href)))))

(deftest rule-matches-test
  (is (rule-matches? [:rule "/match/me?hello=world this is cool"] {:uri "/match/me" :query-string "hello=world%20this%20is%20cool"}))
  (is (rule-matches? [:rule #"/match/(\w+)"] {:uri "/match/me" :query-string ""}))
  (is (not (rule-matches? [:rule #"/match/(\w+)"] {:uri "/match/me/to" :query-string nil})))
  (is (rule-matches? [:rule (fn [& rest] true)] {:uri "/"}))
  (is (not (rule-matches? [:rule (fn [& rest] false)] {:uri "/"}))))

(deftest rewite-test
  (let [handler (wrap-rewrite identity
                              [:rewrite "/match/me?hello=world" "/match?hello=world"]
                              [:rewrite "/match/me" "/match"]
                              [:rewrite #"/match/foo\?(.+)" "/match/you?$1"])]
    (are [req u qs] (let [{:keys [uri query-string]} (handler req)]
                      (is (= uri u))
                      (is (= query-string qs)))
         (request :get "/match/me" {:hello "world"}) "/match" "hello=world" 
         (request :get "/match/me") "/match" nil 
         (request :get "/match/foo" {:hello "world" :q "whatever"}) "/match/you" "hello=world&q=whatever" )))

(run-tests)

;; {:ssl-client-cert nil,
;;  :remote-addr "0:0:0:0:0:0:0:1%0",
;;  :scheme :http,
;;  :request-method :get,
;;  :query-string "hello=world",
;;  :content-type nil,
;;  :uri "/foo",
;;  :server-name "localhost",
;;  :headers
;;  {"accept-encoding" "gzip,deflate,sdch",
;;   "cache-control" "max-age=0",
;;   "connection" "keep-alive",
;;   "user-agent"
;;   "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.43 Safari/537.31",
;;   "accept-language" "en-US,en;q=0.8",
;;   "accept-charset" "ISO-8859-1,utf-8;q=0.7,*;q=0.3",
;;   "accept"
;;   "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
;;   "host" "localhost:3000",
;;   "cookie" "ring-session=777c0230-9427-4361-8732-dd88bf380d9b"},
;;  :content-length nil,
;;  :server-port 3000,
;;  :character-encoding nil,
;;  :body #<HttpInput org.eclipse.jetty.server.HttpInput@7f5960fd>}