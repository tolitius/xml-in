(ns xml-in.core)

(defn filter-all
  "filters all keys 'k' with value matching 'pred'"
  [k pred]
  (filter (comp pred k)))

(defn filter-first
  "finds the first value at the key 'k' matching 'pred'
   and shortcuts transducing"
  [k pred]
  (fn [rf]
    (fn
      ([] (rf))
      ([result] (rf result))
      ([result input]
       (when (-> input k pred)
         (reduced (rf result input)))))))

(defn find-by
  "looks for a key 'k' value that matches the 'pred'
   and returns its child nodes.
   takes an optional 'finder' transducer.
   by default it will filter (i.e. find/return) all the matching values"
  ([k pred]
   (find-by k pred filter-all))
  ([k pred finder]
   (comp (finder k pred)
         (mapcat :content))))   ;; will return child nodes

(defn tag=
  "finds child nodes under all tags 'tag'"
  [tag]
  (find-by :tag (partial = tag)))

(defn some-tag=
  "finds child nodes under the first matching tag 'tag'"
  [tag]
  (find-by :tag
           (partial = tag)
           filter-first))

(defn attr=
  "finds child nodes under all tags with attribute's key 'k' and value 'v'"
  [k v]
  (find-by :attrs (comp (partial = v) k)))

(defn find-in
  "takes the parsed XML (i.e. 'dom')
   and a 'tpath' which is a sequence of transducers.
  
   computes a sequence from the application of all the transducers composed
  
   i.e. (find-in dom [(tag= :universe)
                      (tag= :galaxy)
                      (some-tag= :planet)])"
  [dom tpath]
  (let [dom (if-not (seq? dom) [dom] dom)]
    (sequence (apply comp tpath)
              dom)))

;; TODO: extend to do attrs
(defn find-all
  "takes the parsed XML (i.e. 'dom')
   and a 'path' which is a sequence tags.
  
   finds child nodes for all matching tags
  
  i.e. (find-all universe [:universe :system :solar :planet])"
  [dom path]
  (find-in dom (map tag= path)))

;; TODO: extend to do attrs
(defn find-first
  "takes the parsed XML (i.e. 'dom')
   and a 'path' which is a sequence tags.
  
   finds child nodes for the first matching tag
  
  i.e. (find-first universe [:universe :system :solar :planet])"
  [dom path]
  (find-in dom (map some-tag= path)))
