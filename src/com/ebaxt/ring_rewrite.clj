(ns com.ebaxt.ring-rewrite
  (:require [ring.util.response :as response]
            [clojure.string :as s]))

(defn- regex? [x]
  (instance? java.util.regex.Pattern x))

(defn do-rewrites [^String html rules]
  (reduce (fn [^String acc [_ from to]]
            (s/replace acc from to))
          html rules))

(defn rewrite [^String html rules]
  (let [tasks (group-by first rules)
        {:keys [rewrite]} tasks]
    (do-rewrites html rewrite)))

(defn rewrite-page [handler & rules]
  (fn [req]
    (let [{:keys [headers body] :as response} (handler req)]
      (assoc response :body (rewrite body rules)))))

(defn construct-url [{:keys [uri query-string]}]
  (if (s/blank? query-string)
    uri
    (str uri "?" query-string)))

(defn rule-matches? [[_ from to] req]
  (let [url (construct-url req)]
    (cond
     (string? from) (= from url)
     (regex? from) (re-matches from url) 
     :else (throw (IllegalArgumentException.
                   (str "Illegal from type in rule, only strings and regexes are supported!"))))))

(defn rewrite-fun [url from to req]
  (if (regex? from)
    (to (re-matches from url) req) 
    (to from req)))

(defn rewrite-str [url from to]
  (if (regex? from)
    (s/replace url from to)
    to))

(defn resolve-rewrite [from to req]
  (let [url (construct-url req)]
    (if (ifn? to)
      (rewrite-fun url from to req)
      (rewrite-str url from to))))

(defn redirect-with-status
  [status url]
  {:status status
   :headers {"Location" url}
   :body ""})

(defmulti apply-rule (fn [rule req]
                       (first rule)))

(defmethod apply-rule :rewrite [[_ from to options] req]
  (let [url (construct-url req)
        path (resolve-rewrite from to req)
        [uri query-string] (s/split path #"\?" 2)]
    [true (assoc req :uri uri :query-string query-string)]))

(defmethod apply-rule :default [[rule-type from to options] req]
  (if (contains? #{:301 :302 :303 :307} rule-type)
    [false (redirect-with-status (Integer. (name rule-type)) (resolve-rewrite from to req))]
    (throw  (IllegalArgumentException. (str "Unsupported rule: " (first rule-type))))))

(defn wrap-rewrite [handler & rules]
  (fn [req]
    (if-let [rule (first (drop-while #((complement rule-matches?) % req) rules))]
      (let [[continue result] (apply-rule rule req)]
        (if continue
          (handler result)
          result))
      (do
        (println "No match!")
        (handler req)))))