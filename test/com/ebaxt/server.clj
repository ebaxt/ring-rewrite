(ns com.ebaxt.server
  (:use [compojure.core]
        [com.ebaxt.ring-rewrite]
        [clojure.pprint])
  (:require [compojure.route :as route]))

(def html-page
  (slurp "resources/foo.html"))

(defroutes app-routes
  (GET "/" [] (str "<h1>Dude, this is boring</h1>"))
  (GET "/resources/people/:name" [name] (throw (UnsupportedOperationException. "Route should never be called")))
  (POST "/resources/people/:name" [name] (str "<h1>Created " name "</h1>"))
  (GET "/people/:name" [name] (str "<h1>Hello " name "</h1>"))
  (GET "/secret/name/:name" [name] (str "<h1>Sorry, that's not it!</h1"))
  (GET "/secret/place/:name" [name] (str "<h1>Congratulations " name ", you know the secret!"))
  (GET "/rewrite" [] html-page)
  (route/not-found "<h1>Page not found</h1>"))

(def redirect-handler
  (fn [req]
    (wrap-rewrite req
                  [:rewrite #"/resources/people/(.+)" "/people/$1" :method :get]
                  [:rewrite #"/secret/name/(.+)" "/secret/place/$1" :if (fn [{:keys [headers]}] (= "42" (get headers  "x-secret"))) :not "/secret/name/bully"])))

(def rewrite-handler
  (fn [req]
    (rewrite-page req
                  [:rewrite #"css/(\w+)" "http://cdn.com/$1"]
                  [:rewrite "http://code.jquery.com" "http://cdn.com"]
                  [:rewrite #"http://(.+)/directory/(\w+)/(\w+)" "http://$1/$2/$3"]
                  [:rewrite #"\".+/(img/\w+)" "\"http://mypics.com/$1"])))

(def test-app (-> app-routes
                  redirect-handler
                  rewrite-handler))

;;curl --header "X-Secret: 42" localhost:3000/secret/name/erik
;;<h1>Congratulations erik, you know the secret!                                                                                                                                                                                           ;;;;curl --header "X-Secret: 42" localhost:3000/secret/name/bully
;;<h1>Sorry, that's not it!</h1>