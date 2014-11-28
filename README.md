# ring-rewrite

Ring middleware for defining and applying rewrite rules. In many cases you can get away with ring-rewrite instead of writing Apache mod_rewrite rules. Inspired by [rack-rewrite](https://github.com/jtrupiano/rack-rewrite).

[![Circle CI](https://circleci.com/gh/ebaxt/ring-rewrite.svg?style=svg)](https://circleci.com/gh/ebaxt/ring-rewrite)


## Why

[Use-cases](https://github.com/jtrupiano/rack-rewrite#use-cases)

## Usage

ring-middleware consists of two handlers, `wrap-rewrite` for handling incoming request and `rewrite-page` for updating the reponse body.

### Request

Define rewrite and redirect rules and apply them to the incoming request.

```clojure
(-> app
  (wrap-rewrite
    [:rewrite "/foo" "/bar"]
    [:rewrite #"/foo/(.+)/(.+)" "/bar/$1/$2"]
    [:301 #"/search\?q=(.+)" "http://www.google.com/search?q=$1"]  
    
    ;Options can be applied to only match specific methods, schemes or hosts
    [:302 "/example" "/moved/example" :host "example.com" :scheme :https :method :get]   
    
    ;Use predicates to decide whether to apply rule
    [:rewrite "/example2" "/moved/example2" :if (fn [req] (= "enabled" (System/getProperty "rewrites")))]
    [:rewrite #"/features.*" "/feature_request" :not "/features"] ;match /features.xml not /features
    [:rewrite #"/secret/name/(.+)" "/secret/place/$1"
    :if (fn [{:keys [headers]}] (= "42" (get headers  "x-secret")))
    :not "/secret/name/bully"]
    
    ;Use a funtion to decide where to redirect/rewrite to
    [:rewrite "/custom/rule" (fn [from req] ...)]
    [:303 #"/custom/rule?q=(.+)" (fn [[_ grp] {:keys [uri] :as req}] ...)]
    
    ;Add headers
    [:307 "/example3" "/moved/example3" :headers {"Cache-Control" "no-cache"}])
    
```

See [tests](https://github.com/ebaxt/ring-rewrite/blob/master/test/com/ebaxt/ring_rewrite_test.clj) for more examples.

### Response (experimental)

Adds support for rewriting the outgoing markup on the fly.

```clojure
(-> app
  (rewrite-page 
    [:rewrite #"css/(\w+)" "http://cdn.com/$1"]))
    
    ;before <link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
    ;after <link href="http://cdn.com/bootstrap.min.css" rel="stylesheet" media="screen">
```

## Install

###Leiningen

    [ring-rewrite "0.1.0"]

###Maven

```xml
<dependency>
  <groupId>ring-rewrite</groupId>
  <artifactId>ring-rewrite</artifactId>
  <version>0.1.0</version>
</dependency>
```

## Todo

:send-file and :x-send-file support

## License

Copyright © 2013 Erik Bakstad

Distributed under the Eclipse Public License, the same as Clojure.
