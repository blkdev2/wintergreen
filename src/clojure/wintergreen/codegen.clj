(ns wintergreen.codegen
  [:use [wintergreen.templating]]
  [:import [org.stringtemplate.v4 ST]])

; Forward decl
(def to-js-st)

(def ^:dynamic js-templates (get-templates "javascript.stg"))

(defn apply-js-template [kw value-map]
  (apply-template (js-templates kw) value-map))

(defn to-js 
  "Converts a program node to JavaScript source code. May return a
  string or nil."
  [nodes]
  (let [code (to-js-st nodes)]
    (if (instance? ST code)
      (.render code)
      code)))

(defn block-structured?
  "Returns true if this is a block-structured node. Block-structured
   nodes have enclosing curly braces, and don't need semicolons when
   nested inside other blocks."
  [node]
  (let [block-nodes #{'function 'for 'while}]
    (if (block-nodes (first node)) true false)))

(defn semicolonize
  "Helper function for use within blocks. Given a sub-node, inspects
   whether or not the sub-node is itself a block. Calls to-js-st recursively
   on the sub-node and, for non-block nodes, wraps the result in a statement
   template to add a semicolon."
  [node]
  (if (block-structured? node)
    (to-js-st node)
    (apply-js-template :statement
                       {:expr (to-js-st node)})))

(defmulti to-js-st
  "Multimethod to convert a program node to a StringTemplate object."
  (fn [x] (if (seq? x) (first x) 'scalar)))

; Function.
(defmethod to-js-st 'function [nodes]
  (let [[_ args & statements] nodes]
    (apply-js-template :function
                       {:args (map to-js-st args)
                        :statements (map semicolonize statements)})))

; Function argument.
(defmethod to-js-st 'arg [nodes] (str (nth nodes 2)))

; Return statement.
(defmethod to-js-st 'return [nodes]
  (apply-js-template :return
                     {:value (to-js-st (second nodes))}))

; Function call.
(defmethod to-js-st 'call [nodes]
  (let [[_ name & args] nodes]
    (apply-js-template :functionCall
                       {:function name
                        :args (map to-js-st args)})))

; Method call.
(defmethod to-js-st 'mcall [nodes]
  (let [[_ object method & args] nodes]
    (apply-js-template :methodCall
                       {:object object
                        :method method
                        :args (map to-js-st args)})))

; If-statement.
(defmethod to-js-st 'if [nodes]
  (let [[_ predicate & statements] nodes]
    (apply-js-template :if
                       {:pred (to-js-st predicate)
                        :statements (map semicolonize statements)})))

; While-loop.
(defmethod to-js-st 'while [nodes]
  (let [[_ predicate & statements] nodes]
    (apply-js-template :while
                       {:pred (to-js-st predicate)
                        :statements (map semicolonize statements)})))

; Variable declaration.
(defmethod to-js-st 'decl [nodes]
  (let [[_ name & value] nodes]
    (if (empty? value)
      (apply-js-template :decl
                         {:name (to-js-st name)})
      (apply-js-template :declInit
                         {:name (to-js-st name)
                          :value (to-js-st (first value))}))))

; Variable assignment.
(defmethod to-js-st 'assign [nodes]
  (let [[_ var-expr value] nodes]
    (apply-js-template :assignment
                       {:var (to-js-st var-expr)
                        :value (to-js-st value)})))

; Binary operation.
(defmethod to-js-st 'binop [nodes]
  (let [[_ op a b] nodes]
    (apply-js-template :binop
                       {:left (to-js-st a)
                        :op (str op)
                        :right (to-js-st b)})))

; Reference to a JavaScript local variable.
(defmethod to-js-st 'local [nodes]
  (str (second nodes)))

; Reference to an object field.
(defmethod to-js-st 'field [nodes]
  (apply-js-template :fieldRef
                     {:obj (to-js-st (nth nodes 1))
                      :field (str (nth nodes 2))}))


(defmethod to-js-st 'array [items]
  (let [vertical (some (fn [it] (== (first it) 'function)) items)]
    (apply-js-template :arrayLiteral
                       {:values (map to-js-st items)
                        :verticalLayoutHint (if vertical true false)})))

; Single-value literal. May be a number or string.
(defmethod to-js-st 'scalar [s]
  (if (string? s)
    (str "\"" s "\"")
    (str s)))

; Built-in function to provide a test result value to the testing framework.
(defmethod to-js-st 'putTestValue [nodes]
  (let [[_ name value] nodes]
    (format "testValues.put(%s, %s)" (to-js-st name) (to-js-st value))))

