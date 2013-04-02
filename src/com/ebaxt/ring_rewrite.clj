(ns com.ebaxt.ring-rewrite
  (:require [ring.util.response :as response]
            [clojure.string :as s]
            [ring.util.codec :refer [url-decode]]))
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
    (str uri "?" (url-decode query-string))))

(defn rule-matches? [[_ from to] req]
  (let [url (construct-url req)]
    (cond
     (string? from) (= from url)
     (regex? from) (re-matches from url)
     (ifn? from) (from url req))))

(defmulti apply-rule (fn [rule req]
                       (first rule)))

                                        ;TOOD core.match or a macro to make it less fugly?
(defmethod apply-rule :rewrite [[_ from to options] req]
  (let [url (construct-url req)]
    (cond
     (and (string? from) (string? to)) (let [[uri query-string] (s/split to #"\?")]
                                         [true (assoc req :uri uri :query-string query-string)])
     (and (regex? from) (string? to)) (let [[uri query-string] (s/split (s/replace url from to) #"\?")]
                                        [true (assoc req :uri uri :query-string query-string)])
     
     :else (throw (IllegalArgumentException. (str "Unsupported rule: " (first rule)))))))

(defmethod apply-rule :default [rule req]
  (throw  (IllegalArgumentException. (str "Unsupported rule: " (first rule)))))

(defn wrap-rewrite [handler & rules]
  (fn [req]
    (if-let [rule (first (drop-while #((complement rule-matches?) % req) rules))]
      (let [[continue result] (apply-rule rule req)]
        (if continue
          (handler result)
          result))
      (handler req))))