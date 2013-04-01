(ns com.ebaxt.ring-rewrite
  (:require [ring.util.response :as response]
            [clojure.string :as s]
            [ring.util.codec :refer [url-decode]]))

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
    (str uri "?" (url-decode query-string))))

(defn rule-matches? [[_ from to] req]
  (let [url (construct-url req)]
    (cond
     (string? from) (= from url)
     (instance? java.util.regex.Pattern from) (re-matches from url)
     (ifn? from) (from url req))))

(defmulti apply-rule (fn [rule req]
                       (first rule)))

(defmethod apply-rule :rewrite [[_ from to options] req]
  (throw (RuntimeException. "Implement me"))
  )

(defmethod apply-rule :default [rule req]
  (throw  (IllegalArgumentException. (str "Unsupported rule: " (first rule)))))

(defn wrap-rewrite [handler & rules]
  (fn [req]
    (if-let [rule (first (drop-while #((complement rule-matches?) % req) rules))]
      (handler (apply-rule rule req))      
      (handler req))))