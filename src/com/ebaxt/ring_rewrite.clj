(ns com.ebaxt.ring-rewrite
  (:require [ring.util.response :as response])
  (:use [net.cgrand.enlive-html]))

(defn render
  "Given a seq of Enlive nodes, return the corresponding HTML string."
  [t]
  (apply str (emit* t)))

(defn rewrite [nodes]
  nodes)

(defn wrap-rewrite [handler]
  (fn [req]
    (let [{:keys [headers body] :as response} (handler req)]
      (assoc response :body (-> body
                                (html-snippet)
                                (rewrite)
                                (render))))))