(ns wintergreen.core
  (:use [wintergreen.codegen] :reload)
  (:require clojure.edn))


(defn read-pj-edn [path]
  (let [reader (java.io.PushbackReader. (java.io.FileReader. path))]
    (loop [program []]
      (let [statement (clojure.edn/read {:eof nil} reader)]
        (if (nil? statement)
          program
          (recur (conj program statement)))))))

(defn compile-pj-edn [path]
  (let [program-string (slurp path)
        program-ast (clojure.edn/read-string program-string)]
    (to-js program-ast)))

