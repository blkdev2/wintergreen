(ns wintergreen.testing)

(def script-engine-manager (new javax.script.ScriptEngineManager))

(defn rhino-engine [] (.getEngineByName script-engine-manager "JavaScript"))

(defn run-test-js-string [program-string]
  (let [engine (rhino-engine)
        bindings (.createBindings engine)
        testValues (new java.util.HashMap)]
    (.put bindings "testValues" testValues)
    (.eval engine program-string bindings)
    testValues))
