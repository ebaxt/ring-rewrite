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
  (str uri (if-not (s/blank? query-string)
             (str "?" query-string))))

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

(defn options-map [coll]
  (reduce (fn [m [k v]]
            (assoc m k v))
          {} (partition 2 coll)))

(defn predicates-matches? [[_ _ _ & options] {:keys [request-method]}]
  (let [{:keys [method]} (options-map options)]
    (if method
      (= method request-method)
      true)))

(defn rule-matches? [[_ from to] req]
  (let [url (construct-url req)]
    (cond
     (string? from) (= from url)
     (regex? from) (re-matches from url) 
     :else (throw (IllegalArgumentException.
                   (str "Illegal 'from' type in rule, only strings and regexes are supported!"))))))

(defn no-matching-rule [rule req]
  (not
   (and
    (predicates-matches? rule req)
    (rule-matches? rule req))))

(defn eval-headers [x]
  {:pre [(or (map? x)
             (ifn? x))]
   :post [(map? %)]}
  (if (map? x) x (x)))

(defn merge-additional-hdrs [resp options]
  (if-let [hdrs (:headers (options-map options))]
    (update-in resp [:headers] merge (eval-headers hdrs))
    resp))

(defn apply-rewrite [[_ from to & options] req]
  (let [[u qs] (-> (resolve-rewrite from to req)
                   (s/split #"\?" 2))
        resp (assoc req :uri u :query-string qs)]
    [true (merge-additional-hdrs resp options)]))

(defn redirect-with-status
  [status url]
  {:status status
   :headers {"Location" url}
   :body ""})

(defn apply-redirect [[rule-type from to & options] req]
  (let [status (Integer. (name rule-type))
        url (resolve-rewrite from to req)]
    [false  (merge-additional-hdrs (redirect-with-status status url) options)]))

(defn dispatch-rule [[rule-type :as rule] req]
  (condp contains? rule-type 
    #{:rewrite} (apply-rewrite rule req)
    #{:301 :302 :303 :307} (apply-redirect rule req)
    (throw  (IllegalArgumentException. (str "Unsupported rule: " rule-type)))))

(defn wrap-rewrite [handler & rules]
  (fn [req]
    (if-let [rule (first (drop-while #(no-matching-rule % req) rules))]
      (let [[continue result] (dispatch-rule rule req)]
        (if continue
          (handler result)
          result))
      (handler req))))