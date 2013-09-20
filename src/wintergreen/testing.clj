(ns wintergreen.testing)

(def script-engine-manager (new javax.script.ScriptEngineManager))

(defn rhino-engine [] (.getEngineByName script-engine-manager "JavaScript"))

(defn rhino-test1 []
  (let [engine (rhino-engine)
        bindings (.createBindings engine)
        testValues (new java.util.HashMap)]
    (.put bindings "testValues" testValues)
    (.eval engine "{\"Hello World.\", 5}" bindings)))
