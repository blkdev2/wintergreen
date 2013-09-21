(ns wintergreen.codegen)

(def indent-level (atom 0))
(defn indent []  (swap! indent-level inc))
(defn outdent [] (swap! indent-level #(max 0 (dec %))))

(defn print-indented-line [s]
  (println (apply str (concat (repeat @indent-level "  ") s))))

(defn print-statement [s]  
  (if (not (empty? s)) (print-indented-line (str s ";"))))

(declare to-js)

(defn program-to-js [program]
  (doseq [statement program]
    (let [expr (to-js statement)]
      (if expr
        (println (str expr ";"))))))

(defn program-to-js-string [program]
  (binding [*out* (new java.io.StringWriter)]
    (program-to-js program)
    (.toString *out*)))

(defmulti to-js (fn [x] (if (list? x) (first x) 'scalar)))

(defmethod to-js 'proc [nodes]
  (let [[_ name args & statements] nodes]
    (print-indented-line (format "var %s = function(%s) {"
                                 name
                                 (apply str (interpose ", " (map to-js args)))))
    (indent)
    (dorun (map (comp print-statement to-js) statements))
    (outdent)
    (print-indented-line "}")))

(defmethod to-js 'arg [nodes] (str (nth nodes 2)))

(defmethod to-js 'return [nodes] (format "return %s" (to-js (second nodes))))

(defmethod to-js 'call [nodes]
  (let [[_ name & args] nodes]
    (format "%s(%s)" name (apply str (interpose ", " (map to-js args))))))

(defmethod to-js 'while [nodes]
  (let [[_ predicate & statements] nodes]
    (print-indented-line (format "while (%s) {" (to-js predicate)))
    (indent)
    (dorun (map (comp print-statement to-js) statements))
    (outdent)
    (print-indented-line "}")))

(defmethod to-js 'decl [nodes]
  (let [[_ type name & value] nodes]
    (if (empty? value)
      (format "var %s" name)
      (format "var %s = %s" name (to-js (first value))))))

(defmethod to-js 'assign [nodes]
  (let [[_ varname value] nodes]
    (format "%s = %s" varname (to-js value))))

(defmethod to-js 'binop [nodes]
  (let [[_ op a b] nodes]
    (format "(%s %s %s)" (to-js a) op (to-js b))))

(defmethod to-js 'var [nodes] (str (second nodes)))

(defmethod to-js 'scalar [s]
  (if (string? s)
    (str "\"" s "\"") 
    (str s)))

(defmethod to-js 'putTestValue [nodes]
  (let [[_ name value] nodes]
    (format "testValues.put(%s, %s)" (to-js name) (to-js value))))
