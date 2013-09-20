(ns wintergreen.core
  (:use [wintergreen.codegen])
  (:require clojure.edn))

(def script-engine-manager (new javax.script.ScriptEngineManager))

(def rhino-engine (.getEngineByName script-engine-manager "JavaScript"))

(defn compile-pj-edn [path]
  (let [program-string (slurp path)
        program-ast (clojure.edn/read-string program-string)]
    (to-js program-ast)))
