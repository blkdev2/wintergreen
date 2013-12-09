(ns wintergreen.ast
  [:require [clojure [zip :as zip]]])

; AST format example

(def tree1
  {:tag :par-block
   :statements [{:tag :function-call
                 :name "print"
                 :args [{:tag :constant :type :string :value "Hello!"}]}
                {:tag :while
                 :condition {:tag :constant
                             :value false}
                 :statements  [{:tag :var-decl
                                :name "a"
                                :value {:tag :constant
                                        :value 0}}
                               {:tag :assign
                                :variable {:tag :local-var :name "a"}
                                :value {:tag :constant :type :int :value 5}}]}]})



; Convert less verbose s-expression syntax into AST format
; (mainly assigning positional arguments to appropriate keys)

(defmacro defsyntax
  "Macro to aid in defining methods for expressions. The given
   tag keyword is used as the dispatch value and automatically
   placed in the output map. Args are unpacked from the remainder
   of the expression, excluding the tag."
  [tag args out-map]
  (let [expr (gensym "expr")]
   `(defmethod expr-to-map ~tag [~expr]
      (let [~(vec (concat ['_] args)) ~expr]
        ~(assoc out-map :tag tag)))))

(defmulti sexpr-to-ast first)

(defn sexprs-to-ast [exprs] (vec (map sexpr-to-ast exprs)))

(defsyntax :function-call [name & args]
  {:name name
   :args (sexprs-to-ast args)})

(defsyntax :constant [v]
  {:value v})

(defsyntax :while [condition & statements]
  {:condition (sexpr-to-ast condition)
   :statements (sexprs-to-ast statements)})

(defsyntax :var-decl [name value]
  {:name name
   :value (sexpr-to-ast value)})

(defsyntax :assign [variable value]
  {:variable (sexpr-to-ast variable)
   :value (sexpr-to-ast value)})

(defsyntax :local-var [name]
  {:name name})

(defsyntax :par-block [statements]
  {:statements (sexprs-to-ast statements)})

(defsyntax :seq-block [statements]
  {:statements (sexprs-to-ast statements)})

;; Demo using S-expression syntax

(def tree3
  (expr-to-map 
   `(:par-block (:function-call "print" (:constant "Hello"))
                (:while (:constant false)
                        (:assign (:local-var "a") (:constant 5))))))

;; Zipper type definition for AST

(defn tree-branch? [node] 
  (contains? #{:while :var-decl :assign :function-call :par-block :seq-block}
             (:tag node)))

(defmulti tree-children :tag)
(defmethod tree-children :var-decl [node] [(:value node)])
(defmethod tree-children :while [node] (concat [(:condition node)] (:statements node)))
(defmethod tree-children :assign [node] ((juxt :variable :value) node))
(defmethod tree-children :function-call [node] (:args node))
(defmethod tree-children :par-block [node] (:statements node))
(defmethod tree-children :seq-block [node] (:statements node))

(defmulti tree-make-node (fn [node children] (:tag node)))

(defn tree-zipper [node]
  (zip/zipper tree-branch? tree-children tree-make-node node))
