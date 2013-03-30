# ring-rewrite

Simple middleware for defining and applying rewrite rules.

## Usage

Configure the ring handler

```clojure
(-> app
  (wrap-rewrite 
    [:rewrite "http://code.jquery.com" "http://cdn.com"]
    [:rewrite "js/" "http://cdn.com/"]
    [:rewrite #"css/(\w+)" "http://cdn.com/$1"]
    [:rewrite #"\".+/(img/\w+)" "\"http://mypics.com/$1"]))
```

Given the above configuration, this snippet

```html
<!DOCTYPE html>
<html>
  <head>
    <title>Bootstrap 101 Template</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="css/bootstrap.min.css" rel="stylesheet" media="screen">
  </head>
  <body>
    <h1>Hello, world!</h1>
    <script src="http://code.jquery.com/jquery.js"></script>
    <script src="js/bootstrap.min.js"></script>
    <img src="http://clojure.org/img/logo.png" />
  </body>
</html>
```

will be transformed into

```html
<!DOCTYPE html>
<html>
  <head>
    <title>Bootstrap 101 Template</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link href="http://cdn.com/bootstrap.min.css" rel="stylesheet" media="screen">
  </head>
  <body>
    <h1>Hello, world!</h1>
    <script src="http://cdn.com/jquery.js"></script>
    <script src="http://cdn.com/bootstrap.min.js"></script>
    <img src="http://mypics.com/img/logo.png" />
  </body>
</html>
```



## License

Copyright © 2013 Erik Bakstad

Distributed under the Eclipse Public License, the same as Clojure.
