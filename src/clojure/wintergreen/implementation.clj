; Implementation tree-transforms
; This bridges the gap between ProcessJ semantics and JS semantics. The
; result is a program tree that can be directly turned into JS code by
; the codegen stage.

; https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_Object-Oriented_JavaScript
; http://www.ibm.com/developerworks/library/j-treevisit/

(ns wintergreen.implementation)

; The main body of work that must go on here is the transformation of a process
; into a javascript object with a series of methods to implement the sections of the
; process and keep track of the suspend state


(defn make-dummy-process
  "Create a dummy process just so we can test the runtime"
  []
  '(decl dummy
         (function ()
              (assign (field this cc) (field this fn1))
              (assign (field this a) 0)
              (assign (field this fn1)
                      (function ()
                                (assign (field this a) (binop + (field this a) 1))
                                (assign (field this cc) (field this fn2))))
              (assign (field this fn2)
                      (function ()
                                (assign (field this a) (binop + (field this a) 2))
                                (assign (field this cc) (field this fn1)))))))
