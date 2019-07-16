(ns clojure-noob.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "I'm a little teapot!"))

(println "Cleanliness is next to godlines")

(defn train
  []
  (println "Choo choo!"))

(+ 1 2 3 4)

(+ 1 (* 2 3) 4)

(+ 1 (* 2 3) 3 4)



(defn my-plus
  [& items]
  (do (println items)
      (if (empty? items)
        0
        (do (println (first items))
            (println (rest items))
            (+ (first items) (my-plus (rest items)))))))

(my-plus 1)


(defn my-partial
  [func & args]
  #(apply func (into %& args)))

(my-partial + 1 2)

((my-partial + 1 2) 4)

((my-partial + 1 2) 4 4 2)


;; define a logger level function by partial
;; this function accept 2 arguments, logger level and message
;; user can define a function only accept the logger level.

(defn logger-level
  [level message]
  (condp = level
    :warn (str "[ warning ], message: " message )
    :bug (str "[ bug ], message: " message )))

(def warn-logger (partial logger-level :warn))

(warn-logger "hello, this is a 1-level bug")


(def vampire-database
  ;; 0~4 is the social security ids
  {0 {:makes-blood-puns? false, :has-pulse? true  :name "McFishwich"}
   1 {:makes-blood-puns? false, :has-pulse? true  :name "McMackson"}
   2 {:makes-blood-puns? true,  :has-pulse? false :name "Damon Salvatore"}
   3 {:makes-blood-puns? true,  :has-pulse? true  :name "Mickey Mouse"}})

(defn vampire?
  ;; input a record of people, judge vampire
  ;; record -> record
  ;; map -> map
  [record]
  (and (= true (:makes-blood-puns? record))
       (= false (:has-pulse? record))
       record))

(defn get-detail-record
  ;; input a SSN to get the detail info about this preson
  ;; SSN -> record
  ;; int -> map
  [social-security-number]
  (get vampire-database social-security-number))

(defn vampire-filter
  ;; input a list of SSNs and people db in which find all the records(vampire
  ;; db) of vampire in that SSNs list
  [ssns]
  (filter vampire? (map get-detail-record ssns)))

(vampire-filter [1 2 3])






