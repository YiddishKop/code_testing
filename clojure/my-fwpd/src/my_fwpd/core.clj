(ns my-fwpd.core
  (:require [ clojure.string :as s ])
  (:gen-class))

;; file -> seq of map

;;suspects.csv
(def filename "suspects.csv")
;; define keys of row-map
(def row-keys [:name :glitter-index])
;; convert string to int for :glitter-index
(defn glidx2str [gli-str] (Integer. gli-str))
;; transform the vals according to different keys
(def kf-map {:name identity :glitter-index glidx2str})


(defn file->seqmaps
  [filename]
  (map (fn [row-vals] (zipmap row-keys (map (fn [k v] ((k kf-map) v)) row-keys row-vals)))
       (map #(s/split % #",") (s/split (slurp filename) #"\n"))
       ))
(file->seqmaps filename) 
;; => ({:name "Edward Cullen", :glitter-index 10}
;;     {:name "Bella Swan", :glitter-index 0}
;;     {:name "Charlie Swan", :glitter-index 0}
;;     {:name "Jacob Black", :glitter-index 3}
;;     {:name "Carlisle Cullen", :glitter-index 6}
;;     {:name "yl", :glitter-index 10}
;;     {:name "yl", :glitter-index 10}
;;     {:name "yl", :glitter-index 10}
;;     {:name "yl", :glitter-index 10})




;; Exercises

;; The vampire analysis program you now have is already decades ahead of anything
;; else on the market. But how could you make it better? I suggest trying the
;; following:

;; 1. Turn the result of your glitter filter into a list of names.

(map :name row-maps)

;; => ("Edward Cullen" "Jacob Black" "Carlisle Cullen")


;; 2. Write a function, append, which will append a new suspect to your list of
;; suspects.

(defn map->file
  [row-map]
  (spit filename (str (s/join "," (vals row-map)) "\n") :append true)
 )
(map->file {:name "yuanxin" :glitter-index 23234})



;; 3. Write a function, validate, which will check that :name and :glitter-index
;; are present when you append. The validate function should accept two arguments:
;; a map of keywords to validating functions, similar to conversions, and the
;; record to be validated.

(defn valid
  [filename]
  (let [last-row (last (file->seqmaps filename))
        not-nil? (complement nil?)]
    (def kvf-map {:name not-nil? :glitter-index not-nil?})
    (some not-nil?
          (map #((get kvf-map %) (get last-row %))
               (keys last-row))))
)
(valid filename)



;; 4. Write a function that will take your list of maps and convert it back to a
;; CSV string. You’ll need to use the clojure.string/join function.

;; Good luck, McFishwich!

;; seq of maps -> file
(def row-maps (file->seqmaps filename))

(defn seqmaps->file
  [seqmaps]
  (spit filename (reduce #(str %1 %2) "" (map #(str (s/join "," %) "\n") (map #(vals %) seqmaps)))
        :append true)
  )
(seqmaps->file row-maps)


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
