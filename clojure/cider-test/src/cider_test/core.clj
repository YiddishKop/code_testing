(ns cider-test.core
  (:gen-class))

(defn fact-rec [n]
  (if (= n 1)
    1
    (* n (fact-rec (dec n)))))

(defn fibo-rec [n]
  (case n
        0 0
        1 1
        (+ (fibo-rec (dec n)) (fibo-rec (- n 2)))))

(defn fibo-iter
  ([n] (fibo-iter 0 1 n))
  ([curr nxt n]
   (cond
     (zero? n) curr
     :else (recur nxt (+ curr nxt) (dec n)))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
