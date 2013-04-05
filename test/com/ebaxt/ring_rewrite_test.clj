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

(def redirect-handler
  (fn [req]
    (wrap-rewrite req
                  [:302 #"/search\?q=(.+)" "http://google.com/search?q=$1"])))

(def test-app (-> rewrite-handler
                  redirect-handler))

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
  (is (rule-matches? [:rule "/match/me?hello=world"] {:uri "/match/me" :query-string "hello=world"}))
  (is (rule-matches? [:rule #"/match/(\w+)"] {:uri "/match/me" :query-string ""}))
  (is (not (rule-matches? [:rule #"/match/(\w+)"] {:uri "/match/me/to" :query-string nil})))
  (is (rule-matches? [:r301 #"/match/me\?hello=(\w+)"] {:uri "/match/me" :query-string "hello=world"})))

(deftest rewite-test
  (let [handler (wrap-rewrite identity
                              [:rewrite "/match/me?hello=world" "/match?hello=world" :headers {"Cache-Control" "no-cache"}]
                              [:rewrite "/match/me" "/match" :headers (fn [] {"Expires" (str (java.util.Date.))})]
                              [:rewrite #"/match/foo\?(.+)" "/match/you?$1"]
                              [:rewrite "/foo/bar" (fn [from req] "/baz/qux")]
                              [:rewrite #"/foo/(bar)/baz" (fn [[_ grp] {:keys [server-name uri]}] (str "http://" server-name "/" uri "/" grp))])]
    (are [req u qs hdrs] (let [{:keys [uri query-string headers]} (handler req)]
                           (is (= (every? #(contains? headers %1) hdrs)))
                           (is (= uri u))
                           (is (= query-string qs)))
         (request :get "/match/me" {:hello "world"}) "/match" "hello=world" #{"Cache-Control"}
         (request :get "/match/me") "/match" nil #{"Expires"}
         (request :get "/match/foo" {:hello "world" :q "whatever"}) "/match/you" "hello=world&q=whatever" #{}
         (request :get "/foo/bar") "/baz/qux" nil #{}
         (request :get "/foo/bar/baz") "http://localhost//foo/bar/baz/bar" nil #{})))

(deftest redirect-test
  (let [handler (wrap-rewrite identity
                              [:301 #"/redirect/301\?hello=(.+)" "http://www.google.com/301?q=$1" :headers {"Cache-Control" "no-cache"}]
                              [:302 #"/redirect/302\?hello=(\w+)" "http://www.google.com/302?q=$1"]
                              [:303 #"/redirect/303\?hello=(\w+)" "http://www.google.com/303?q=$1"]
                              [:307 #"/redirect/307\?hello=(\w+)" "http://www.google.com/307?q=$1"])]
    (are [req sts location hdrs] (let [{:keys [status headers]} (handler req)]
                                   (is (every? #(contains? headers %1) hdrs))
                                   (is (= status sts))
                                   (is (= (get headers "Location") location)))
         (request :get "/redirect/301" {:hello "clojure rocks"}) 301 "http://www.google.com/301?q=clojure+rocks" #{"Cache-Control" "Location"}
         (request :get "/redirect/302" {:hello "clojure"}) 302 "http://www.google.com/302?q=clojure" #{"Location"}
         (request :get "/redirect/303" {:hello "clojure"}) 303 "http://www.google.com/303?q=clojure" #{"Location"}
         (request :get "/redirect/307" {:hello "clojure"}) 307 "http://www.google.com/307?q=clojure" #{"Location"})))

(deftest options-test
  (let [handler (wrap-rewrite identity
                              [:rewrite "/foo" "/no_longer_available.html" :method :post]
                              [:rewrite "/baz" "/qux" :host "foobar.com"]
                              [:rewrite "/both" "/both/correct" :host "ebaxt.com" :method :post]
                              [:rewrite "/http" "/https" :scheme :https])]
    (are [path req] (is = (:uri (handler req)))
         "/no_longer_available.html"  (request :post "/foo")
         "/foo" (request :get "/foo")
         "/baz"  (request :get "/baz")
         "/qux" (header (request :get "/baz" ) "host" "foobar.com")
         "/both" (request :post "/both")
         "/both" (header (request :get "/both" ) "host" "ebaxt.com")
         "/both/correct" (header (request :post "/both") "host" "ebaxt.com")
         "/http" (request :get "/http")
         "/https" (assoc (request :get "/http") :scheme :https))))

(deftest predicate-test
  (let [handler (wrap-rewrite identity
                              [:rewrite "/foo" "/example" :if (fn [req] (= "example.com" (get-in req [:headers "host"])))]
                              [:rewrite #"/features.*" "/feature_request" :not "/features" :scheme :https :if (fn [req] (= "example.com" (get-in req [:headers "host"])))])]
    (are [path req] (is (= path (:uri (handler req)))) 
         "/foo" (request :get "/foo")
         "/example" (header (request :get "/foo") "host" "example.com")
         "/foo" (header (request :get "/foo") "host" "foo.com")
         "/features" (request :get "/features")
         "/features.xml"  (request :get "/features.xml")
         "/features.xml" (header (request :get "/features.xml") "host" "example.com")
         "/feature_request" (assoc (header (request :get "/features.xml") "host" "example.com") :scheme :https))))

;; (deftest send-file-test
;;   (let [handler (wrap-rewrite identity
;;                               [:send-file #".+/(.+.(png|jpg|mov))" "/resources/img/$1"])]
;;     (handler (request :get "/foo/bar/baz.png"))))
