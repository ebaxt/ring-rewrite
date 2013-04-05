# ring-rewrite

Ring middleware for defining and applying rewrite rules. In many cases you can get away with ring-rewrite instead of writing Apache mod_rewrite rules. Inspired by [rack-rewrite](https://github.com/jtrupiano/rack-rewrite).

![Build Status](https://travis-ci.org/ebaxt/ring-rewrite.png)

## Why

[Use-cases](https://github.com/jtrupiano/rack-rewrite#use-cases)

## Usage

ring-middleware consists of two handlers, `wrap-rewrite` for handling incoming request and `rewrite-page` for updating the reponse body.

### Request

Define rewrite and redirect rules and apply them to the incoming request.

```clojure
(-> app
  (wrap-rewrite
    [:rewrite "/foo/bar.html" "/archive/bar.html" :headers (fn [] {"Expires" (str (java.util.Date.))})]
    [:rewrite "/baz" "/qux" :host "foobar.com"]
    [:301 #"/search/\?q=(.+)" "http://www.google.com/search?q=$1" :method :get}])
```

### Response (experimental)

Adds support for rewriting the outgoing markup on the fly.

```clojure
(-> app
  (rewrite-page 
    [:rewrite #"css/(\w+)" "http://cdn.com/$1"]))
    
    ;before <link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
    ;after <link href="http://cdn.com/bootstrap.min.css" rel="stylesheet" media="screen">
```

## License

Copyright © 2013 Erik Bakstad

Distributed under the Eclipse Public License, the same as Clojure.
