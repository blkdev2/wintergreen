(ns wintergreen.templating
  [:require [clojure.java.io :as io]]
  [:import [org.stringtemplate.v4 ST STGroup STGroupFile]])


(defn template [args str]
  [args (ST. str \< \>)])


(defn- kw-to-name [kw] (apply str (rest (str kw))))

(defn get-templates
  "Takes a resource file name in /resources, loads the StringTemplate
   group, and wraps it in a closure to return ST instances for keywords
   corresponding to template names. e.g.
   ((get-templates <file>) :mytemplate)"
  [resource-name]
  (let [url (io/resource resource-name)
        path (.getPath url)
        stg (STGroupFile. path)
        names (apply vector (.getTemplateNames stg))
        cleanup (fn [name] (if (= (first name) \/)
                             (apply str (rest name))
                             name))
        keywords (set (map (comp keyword cleanup) names))]
    (fn [kw] (if (keywords kw)
               (.getInstanceOf stg (kw-to-name kw))
               nil))))

(defn apply-template
  "Takes a StringTemplate ST object and a map from keywords to values, fills
   template parameters according to keyword names."
  [template arg-map]
  (doseq [name (keys arg-map)]
    (.add template (kw-to-name name) (arg-map name)))
  template)

(defn render-template
  "Takes a StringTemplate ST object and a map from keywords to values, fills
   template parameters according to keyword names, and renders the template."
  [template arg-map]
  (.render (apply-template template arg-map)))

