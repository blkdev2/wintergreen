(ns wintergreen.codegen)

(def indent-level "Current source-code indentation level." (atom 0))

(defn indent "Increment current indentation level." []
  (swap! indent-level inc))

(defn outdent "Decrement current indentation level." []
  (swap! indent-level #(max 0 (dec %))))

(defn print-indented-line
  "Print a string indented to the level defined in the var indent-level."
  [s]
  (println (apply str (concat (repeat @indent-level "  ") s))))

(defn print-statement
  "Print a string indented to current indent-level with trailing semicolon."
  [s]
  (if (not (empty? s)) (print-indented-line (str s ";"))))

(declare to-js)

(defn write-program-js
  "Top-level driver to write a program tree as JavaScript source
  code. Writes code to *out*."
  [program]
  (binding [indent-level (atom 0)]
    (doseq [statement program]
      (let [expr (to-js statement)]
        (if expr
          (println (str expr ";")))))))

(defn program-to-js
  "Converts a program tree to JavaScript source code, returned as a string."
  [program]
  (binding [*out* (new java.io.StringWriter)]
    (write-program-js program)
    (.toString *out*)))

(defmulti write-js 
  "Converts a program node to JavaScript source code. May return a
  string or nil.

  In general, statement blocks are responsible for printing their code
  to *out*, properly separating statements with semicolons and
  newlines. Other tree nodes will return strings."
  (fn [x] (if (list? x) (first x) 'scalar)))

; Procedure.
(defmethod write-js 'proc [nodes]
  (let [[_ name args & statements] nodes]
    (print-indented-line (format "var %s = function(%s) {"
                                 name
                                 (apply str (interpose ", " (map write-js args)))))
    (indent)
    (dorun (map (comp print-statement write-js) statements))
    (outdent)
    (print-indented-line "}")))

; Procedure argument.
(defmethod write-js 'arg [nodes] (str (nth nodes 2)))

; Return statement.
(defmethod write-js 'return [nodes] (format "return %s" (write-js (second nodes))))

; Procedure call.
(defmethod write-js 'call [nodes]
  (let [[_ name & args] nodes]
    (format "%s(%s)" name (apply str (interpose ", " (map write-js args))))))

; While-loop.
(defmethod write-js 'while [nodes]
  (let [[_ predicate & statements] nodes]
    (print-indented-line (format "while (%s) {" (write-js predicate)))
    (indent)
    (dorun (map (comp print-statement write-js) statements))
    (outdent)
    (print-indented-line "}")))

; Variable declaration.
(defmethod write-js 'decl [nodes]
  (let [[_ type name & value] nodes]
    (if (empty? value)
      (format "var %s" name)
      (format "var %s = %s" name (write-js (first value))))))

; Variable assignment.
(defmethod write-js 'assign [nodes]
  (let [[_ varname value] nodes]
    (format "%s = %s" varname (write-js value))))

; Binary operation.
(defmethod write-js 'binop [nodes]
  (let [[_ op a b] nodes]
    (format "(%s %s %s)" (write-js a) op (write-js b))))

; Variable.
(defmethod write-js
  'var [nodes] (str (second nodes)))

; Single-value literal. May be a number or string.
(defmethod write-js 'scalar [s]
  (if (string? s)
    (str "\"" s "\"") 
    (str s)))

; Built-in function to provide a test result value to the testing framework.
(defmethod write-js 'putTestValue [nodes]
  (let [[_ name value] nodes]
    (format "testValues.put(%s, %s)" (write-js name) (write-js value))))
