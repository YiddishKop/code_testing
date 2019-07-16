(ns fwpd.chap5)

;; Pure Functions Are Referentially Transparent
;; ============================================

;; 我们目前只学习到了两个 "非纯函数": println 和 rand: 其中 println 会产生副作用,
;; rand 是输入相同的参数会产生不同的运算结果.

(println "hello world")
;; => nil

(rand 3) 
;; => 0.7659650044262647

(rand 3) 
;; => 2.113156016635517


;; 一个函数只依赖[ 不变的量 ]和[ 自己的参数 ]来运算出结果, 这样的函数就具
;; 有 "reference transparent" 性质. eg:

(defn wisdom
  [words]
  (str words ", Daniel-san"))

(wisdom "Always bathe on Fridays")

;; 如果一个函数[ 依赖了某个变量 ], 那么这个函数就是[ 不纯函数 ]:

(defn year-end-evaluation
  []
  (if (> (rand) 0.5)
    "you get a raise!"
    "Better luck next year!"))

(year-end-evaluation) 
;; => "you get a raise!"

(year-end-evaluation) 
;; => "Better luck next year!"

;; 如果一个函数[ 依赖某个从文件中读取 ]的内容, 那么这个函数也是[ 不纯函数 ], 因
;; 为文件内容会改变. 如下所示, 相同的文件名会产生不同的内容, 因为文件内容改变,
;; 函数无法感知. 但如果如 analysis 一样接受的参数是文本内容, 则仍然是 reference
;; transparent. 

(defn analyze-file
  [filename]
  (analysis (slurp filename)))

(defn analysis
  [text]
  (str "Character count: " (count text)))


;;  Living with Immutable Data Structures
;;  =====================================

;; How to get things done without side effects:

;; (1) recursion instead of for/while

(defn my-sum
  [lst]
   (if (empty? lst)
     0
     (+ (first lst) (my-sum (rest lst)))))

(my-sum [1 2 3])


(defn my-sum2
  [lst]
  (loop [lstx lst
         target 0]
    (if (empty? lstx)
      target
    (recur (rest lstx) (+ target (first lstx))))))

(my-sum2 [1 2 3])

;; 这里有一个我之前没见过的组合 fn-args + recur, 这个也很重要, 一般用来替代
;; recursion, 也就是说 recur 可以有两种组合方式:
;; (1) loop + recur
;; (2) fn-args + recur

(defn book-sum
  ([vals]
   (sum vals 0))
  ([vals accumulated-total]
   (if (empty? vals)
     accumulated-total
     (recur (rest vals) (+ accumulated-total (first vals))))))



;; Function Composition Instead of Attribute Mutation
;; ==================================================

(require '[clojure.string :as s])

(defn clean
  [text]
  (s/replace (s/trim text) #"lol" "LOL"))

(clean "My boa constrictor is so sassy lol! ")

;; Cool Things to Do with Pure Functions
;; =====================================

;; comp
;; ----

((comp inc *) 2 3) ;; comp 的函数顺序就是 (inc (* 2 3))

((comp inc max) 1 2 3)


;; Here's an example that shows how you could use ~comp~ to retrieve character
;; attributes in role-playing games.

(def character
  {:name "smooches mccutes"
   :attributes {:intelligence 10
                :strength 4
                :dexterity 5}})

(def c-int (comp :intelligence :attributes))
(def c-str (comp :strength :attributes))
(def c-dex (comp :dexterity :attributes))

(c-int character)
(c-dex character)
(c-str character)


;; 通过上面你的例子 eg (comp inc *) 可以发现, 靠后面的函数只允许有一个参数, 也就是前面的函数的结果,
;; (* a b) => prod, (inc prod). 如果我想把一个接收 2 个参数的函数与 * 做组合怎么办: 那就通过
;; 匿名函数先给这个函数一个参数,把这个函数变成一个单参数函数.


(defn spell-slots
  [char]
  (int (inc (/ (c-int char) 2))))

(spell-slots character) 
;; => 6

;; 通过 #(/ % 2) 把 "/" 变成一个单参数函数.
((comp int inc #(/ % 2) c-int) character) 
;; => 6

;; 实现一个双参数 comp 函数

((comp inc *) 3 3)


;; memoize
;; -------

;; 因为纯函数具有 reference transparent 的特性, 也就是填入相同的参数给纯函数运算
;; 结果也会是相同的, 如此以来我们既然运算结果是相同的, 下一次再输入该参数就没必
;; 要再算一次, memoize 的意思就是保存运算结果, 供下一次再次以相同参数调用时使用.

(+ 3 (+ 5 8))

(+ 3 13)

16

;; Without the memoize.
(defn sleepy-identity
  "return the given value after 1 second"
  [x]
  (Thread/sleep 1000)
  x)

(sleepy-identity "Mr. Fantastico") ;; waiting 1 second before string display

(sleepy-identity "Mr. Fantastico") ;; still waiting 1 second before string display


;; With memoize

(def memo-sleep-identity (memoize sleepy-identity))

(memo-sleep-identity "Mr. Fantastico") ;; waiting 1 second before string display

(memo-sleep-identity "Mr. Fantastico") ;; no waiting, display string immediately


