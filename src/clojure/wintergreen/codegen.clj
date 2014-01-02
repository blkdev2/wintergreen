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
  (fn [x] (if (map? x) (:tag x) :scalar)))

; Function.
(defmethod to-js-st :function [node]
  (apply-js-template :function
                     {:args (map to-js-st (:args node))
                      :statements (map semicolonize (:statements node))}))

; Function argument.
(defmethod to-js-st :arg [node] (str (:name node)))

; Return statement.
(defmethod to-js-st :return [node]
  (apply-js-template :return
                     {:value (to-js-st (:value node))}))

; Function call.
(defmethod to-js-st :function-call [node]
  (apply-js-template :functionCall
                     {:function (:name node)
                      :args (map to-js-st (:args node))}))

; Method call.
(defmethod to-js-st :method-call [node]
  (apply-js-template :methodCall
                     {:object (:object node)
                      :method (:method node)
                      :args (map to-js-st (:args node))}))

; If-statement.
(defmethod to-js-st :if [node]
  (apply-js-template :if
                     {:pred (to-js-st (:predicate node))
                      :statements (map semicolonize (:statements (:block node)))}))

; While-loop.
(defmethod to-js-st :while [node]
  (apply-js-template :while
                     {:pred (to-js-st (:predicate node))
                      :statements (map semicolonize (:statements (:block node)))}))

; Variable declaration.
(defmethod to-js-st :decl [node]
  (if (contains? node :value)
    (apply-js-template :declInit
                       {:name (to-js-st (:name node))
                        :value (to-js-st (:value node))})
    (apply-js-template :decl
                       {:name (to-js-st (:name node))})))

; Variable assignment.
(defmethod to-js-st :assign [node]
  (apply-js-template :assignment
                     {:var (to-js-st (:variable node))
                      :value (to-js-st (:value node))}))

; Binary operation.
(defmethod to-js-st :binop [node]
  (apply-js-template :binop
                     {:left (to-js-st (:left node))
                      :op (str (:operator node))
                      :right (to-js-st (:right node))}))

; Reference to a JavaScript local variable.
(defmethod to-js-st :local [node]
  (:name node))

; Reference to an object field.
(defmethod to-js-st :field [node]
  (apply-js-template :fieldRef
                     {:obj (to-js-st (:object node))
                      :field (:field node)}))


(defmethod to-js-st :array [node]
  (let [items (:items node)
        vertical (some (fn [it] (== (:tag it) :function)) items)]
    (apply-js-template :arrayLiteral
                       {:values (map to-js-st items)
                        :verticalLayoutHint (if vertical true false)})))

; Single-value literal. May be a number or string.
(defmethod to-js-st :scalar [s]
  (if (string? s)
    (str "\"" s "\"")
    (str s)))

; Built-in function to provide a test result value to the testing framework.
(defmethod to-js-st :putTestValue [node]
  (format "testValues.put(%s, %s)" (to-js-st (:name node)) (to-js-st (:value node))))

