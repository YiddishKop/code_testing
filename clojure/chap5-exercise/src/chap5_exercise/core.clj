(ns chap5-exercise.core
  (:gen-class))

;;;;;;;;;;;;;;
;; 课后练习 ;;
;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 1: implment comp ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; by reduce
(defn my-comp
  ([] identity)
  ([f1] f1)
  ([f1 f2 & fs] (fn [val] (reduce (fn [fn1 fn2] (fn2 (fn1 val))) (reverse (concat [f1 f2] fs)))))
  )
((my-comp inc) 3)
((my-comp) 1)

;; by recur
(defn my-comp
  [& fs]
  (if (empty? fs) identity
      (fn [val] (loop [[hf & tfs] fs
                       r (hf val)]
                  (if (empty? tfs) r
                      (recur tfs ((first tfs) r)))))))
((my-comp inc) 3)
((my-comp) 1)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 2: implment assoc-in ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn my-assoc-in
  ([mp [k & ks] v]
   (if (empty? ks) (assoc mp k v)
       (recur (get mp k) ks v))))

;; (assoc-in {:a {:b 1} :c 2} :b 2)
;; =>
;; (get {:a {:b 1}} :a) => {:b 1}
;; =>
;; (assoc {:b 1} :b 2) => {:b 2}
;; => (assoc {} :a {:b 2})



(defn my-assoc-in
  [mp [k & ks] v]
  (if (empty? ks) (assoc mp k v)
      (assoc mp k (my-assoc-in (get mp k) ks v))))





(my-assoc-in {} [:a :b] 1) ;; => {:a {:b 1}}
(my-assoc-in {:a 1 :b 2} [:a] 3) ;; => {:a 3 :b 2}
(my-assoc-in {:a {:b 2} :c 1} [:a :b] 1) ;; => {:a 3 :b 2}



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 3: look-up and use update-in ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; (update-in [mp [& ks] f & args])





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 3.1: update-in统计一个字符串中每个字母出现的次数 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn count-word
  [s]
  (let [lm (zipmap (map  (comp str char) (range (int \a) (inc (int \z)))) (repeat 26 0))]
    (reduce #(update-in %1 [(str %2)] inc) lm s))
  )

(count-word "sdfsdfsdf")

;; 能够针对【匹配到的key】的值进行【运算】。update-in 包含两个功能，匹配和运算。

;; [编程感想]
;; 如果要问有了 assoc-in 为什么还需要 update-in，最重要的就是 update-in 可以使用
;; 函数，并把 assoc-in 作为该函数的参数。




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 4: implment update-in ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn my-update-in
  [mp ks f & args]
  (let [ [hk & tk] ks]
    (if (empty? tk)
      (assoc mp hk (apply f (get mp hk) args))
      (assoc mp hk (apply my-update-in (get mp hk) tk f args))
      )))



(my-update-in {:a 1 :b {:c {:d 4}}} [:b :c :d] + 3) ;; => {:a 1 {:b {:c {:d 5}}}}

;; mp1 -> (get mp1 [*:b* :c :d]) -> mp2

;; 递归模式推导：
;; (assoc mp1 :b
;;        (assoc mp2 :c
;;               (assoc mp3 :d
;;                      (inc (get mp3 :d)))))
;;--------------------------------------------
;; (defn f [___] (assoc (f ___)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 5: implment apply ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; [编程感想]
;; 如何实现从单一参数，到多个参数的自动适配.
;; 或者说当参数是 nil 时，如何不让其参与运算，同时使得函数被看成单参数函数。
;; TODO 看了 apply 源码发现有一些自己不清楚的语法，留待以后回头看。



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Exercise 6: look-up and use fnil ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; fnil 返回一个函数
;; (fnil func value) = if give function 'func' nil as 'arg', then give 'arg' the 'value'
;; fnil 的本质是【当函数参数为nil时，给函数另一个参数】, 比如如下的例子，当


;; if we give nil to response, then it will only give "hello "







(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
