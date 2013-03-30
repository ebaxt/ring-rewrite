(ns com.ebaxt.ring-rewrite
  (:import java.util.regex.Pattern)
  (:require [ring.util.response :as response]
            [clojure.string :as s]))

(defn do-rewrites [^String html rules]
  (reduce (fn [^String acc [_ from to]]
            (s/replace acc from to))
          html rules))

(defn rewrite [^String html rules]
  (let [tasks (group-by first rules)
        {:keys [rewrite]} tasks]
    (do-rewrites html rewrite)))

(defn wrap-rewrite [handler & rules]
  (fn [req]
    (let [{:keys [headers body] :as response} (handler req)]
      (assoc response :body (rewrite body rules)))))