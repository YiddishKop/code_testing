(ns mock-pegthing.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))


;;;;
;; Create the board
;;;;

;; board 的数据结构是一个大的 map:
;; {
;;  :rows 4
;;  1 {:pegged true
;;     :connect {4 2
;;               6 3}}
;;  2 {:pegged true
;;     :connect {7 4
;;               9 5}}
;;  3 {:pegged true
;;     :connect {8 5
;;               10 6}}
;;  4 {:pegged true
;;     :connect {1 2
;;               6 5}}
;;  ...
;;  }


;; 

(defn tri*
  "无限集合为什么好用, 就在于他内涵了数列规律, 且可以暂时忽略数列长度,
  我们一般想获得一个数列的逻辑是考虑如何满足规律并且满足数列长度要求.
  在 clojure 中这完全可以通过两个非常简单的步骤来实现:
  1. 只关注数列规律,生成无限集
  2. 从无限集中根据[ 过滤条件 ]截取你要的长度

  这是 clojure 区别于传统面向过程和OO的语言在处理数列时非常大的思维方式上的区别,
  因为传统语言模式, 是没法获取无限集对象的, 自然无法拆成两步.

  比如你想拿到前100个偶数数列, 或者前100个fiboncci数列,都可以先通过规律生成
  一个无限集, 然后截取前100个.

  这个函数就是(按层)获取棋盘上的点的个数的无限集

          -> inf seq
  int int -> inf seq"
  ([] (tri* 0 1))
  ([sum n]
   (let [new-sum (+ sum n)]
     (cons new-sum (lazy-seq (tri* new-sum (inc n)))))))


(def tri (tri*))

(defn triangular?
  "[1 3 6 10 15 21 28 ...]
  input: 13
  output: 10

  这个函数可以用来进行[ 行尾检测 ]

  输入一个数字判断他是否能完整构成一个三角棋盘

  int -> bool
  "
  [n]
  (= n (last
        (take-while #(>= n %) tri) ;; 这个函数就是根据[过滤条件]从无限集中获取子集
             )))

;; 无限集的使用举例
;; 比如: 获取前 100 个偶数组成的数列
;; =============================
(defn even-lseq
  "获取偶数无限集

      -> inf seq
  int -> inf seq"
  ([] (even-lseq 0))
  ([even-num]
   (let [new-even (+ 2 even-num)]
     (cons even-num (lazy-seq (even-lseq new-even))))))

(take 10 (even-lseq)) ;; 获取无限集中的前100个
;; =============================


(defn row-tri
  "输入行数, 输出棋盘大小

  int -> int"
  [n]
  (last (take n tri)))

(defn row-num
  "根据输入的棋盘位置, 输出该位置所在的行数, 注意:位置计数与行数都从1开始

  int -> int"
  [pos]
  (inc (count (take-while #(>= pos %) tri))))

(defn in-bounds?
  "apply 就相当于一个爆破函数, 他可以把一个列表爆破成一个个的元素.

  该函数判断一个位置是不是行尾.

  seq -> bool"
  [max-pos & positions]
  (= max-pos (apply max max-pos positions)))


(defn connect
  "
  [新函数介绍]
  assoc-in 接受三个参数,
  1. seq of seq
  2. [索引位置]
  3. 值
  assoc-in 就相当于改变数组值的一个函数, 不论数组是以什么形式都可以更改,
  索引位置不存在的就[ 插入 ], 索引位置存在的就[ 更改 ].

  assoc-in 是创建复杂嵌套 map 的非常非常重要的工具, 他可以无限制的创建
  嵌套 map, 由于该函数对 map 而言没有索引超界担心, 所以是创建复杂 map
  的不二人选.

  对于数组来说, 索引超出界限就报错. 对于 map 来说, 键就相当于索引位置, 相当于数
  组或列表的索引, 如果键不存在就会创建一个新的键值对.

  本函数是把三个点通过 board 的 :connect 键值进行连接.
  估计:
  0. 每个点都用一个 map 表示, 已经知道的是每个点的 map 都有一个 :connect 键.
  1. :connect 的值应该是一个 vector
  2. 该 vector 的索引表示了目标位置, 该索引的内容表示中间元素.

  map, int, int ,int, int -> map
  "
  [board max-pos pos neighbor destination]
  (if (in-bounds? max-pos neighbor destination)
    (reduce (fn [new-board [p1 p2]]
              (assoc-in new-board [p1 :connect p2] neighbor))
            board
            [[pos destination] [destination pos]])
    board))

;;;;;
;; 下面的三个建立连接关系的函数, 一个向右, 一个向左下,一个向右下. 
;; [Q]问题是为什么没有向左, 左上, 右上的 connect 函数:
;;
;;             ^     ^
;;              .   .
;;               . .
;;         < . . .*------>
;;               / \
;;              /   \
;;             v     v
;; [A]因为每个函数都是基于 connect 函数, 而该函数建立的是双向连接
;; [Q]而且为什么只有向右的连接函数, connect-right 有边界判断, 其余两个都没有
;; [A]因为 connect-right 需要保证 neighbor 和 destination 不能处在行尾,
;;    而向左下和右下没有这个要求, 左下和右下直接调用 connect 函数, 该函数已经
;;    有边界检测了.
;;;;;


(defn connect-right
  "向右根据每个点建立连接, 首先需要检测两种情况:
  1. 该位置 pos 不是行尾
  2. 该位置的下一个位置 neighbor 也不是行尾

  map, int, int -> map"
  [board max-pos pos]
  (let [neighbor (inc pos)
        destination (inc neighbor)]
    (if-not (or (triangular? neighbor) ;; 边界检测 neighbor 和 destination 都不能
                (triangular? max-pos pos)) ;; 处在行尾位置
      (connect board max-pos pos neighbor destination)
      board)))



(defn connect-down-left
  "这里为什么不再判断是否是整个棋盘的最后一行

  这里有个有意思的东西, 上下层之间的关系, 是我之前没发现的:
           1                1
         2   3              2
       4   5   6            3
     7   8   9   10         4
   11  12  13  14  15       5

  2 = 2
  4 = 2 + row(2)
  7 = 2 + row(2) + 1

  4 = 4
  7 = 4 + row(4)
  11= 4 + row(4) + 1
  "
  [board max-pos pos]
  (let [row (row-num pos)
        neighbor (+ row pos)
        destination (+ 1 row pos)]
    (connect board max-pos pos neighbor destination)))


(defn connect-down-right
  "
  这里为什么不再判断是否是整个棋盘的最后一行

  "
  [board max-pos pos]
  (let [pos-row (row-num pos)
        neighbor (+ pos pos-row 1)
        neighbor-row (row-num neighbor)
        destination (+ neighbor neighbor-row 1)]
    (connect board max-pos pos neighbor destination)))

(defn add-pos
  "该函数实现两个功能:
  1. 给指定的一个位置 pos 钉入楔子--- :pegged 标志位设为 true
  2. 建立该位置 pos 的 6 个连接关系

  map, int, int -> map"
  [board max-pos pos]
  (let [pegged-board (assoc-in board [pos
                                      ;; 如果被插入的集合board是一个数组, 这个pos就是索引, 且不可越原始数组之边界
                                      ;; 如果被插入的集合board是一个map, 这个pos就是键, 无需考虑越界问题
                                      ;; 很明显这里是后者.

                                      ;; (assoc-in {:rows 4} [1 :pegged] true)
                                      ;; => {:rows 4, 1 {:pegged true}}

                                      ;; (assoc-in [] [1 :pegged] true) Execution
                                      ;; => error (IndexOutOfBoundsException) at
                                      ;; => mock-pegthing.core/eval17349 (form-init1966824160783829050.clj:240).


                                      :pegged] true)] ;; 将部分工作放在 let 的赋值语句中, 可以实现一个函数实现两种功能
    (reduce (fn [cache-board connector] (connector cache-board max-pos pos))
            board
            [connect-right connect-down-left connect-down-right])))

;; board 的数据结构是一个大的 map:
;; {
;;  :rows 4
;;  1 {:pegged true
;;     :connect {4 2
;;               6 3}}
;;  2 {:pegged true
;;     :connect {7 4
;;               9 5}}
;;  3 {:pegged true
;;     :connect {8 5
;;               10 6}}
;;  4 {:pegged true
;;     :connect {1 2
;;               6 5}}
;;  ...
;;  }

(defn new-board
  "输入行数,输出一个空的 board

  board 的本质是一个 seq of map

  由这里可以看出, max-pos 是指整个棋盘最后一个位置.
  "
  [rows]
  (let [initial-board {:rows rows}
        max-pos (row-tri rows)]
    (reduce (fn [board pos]
              (add-pos board max-pos pos))
            initial-board
            (range 1 (inc max-pos)))))


;;;;
;; Move pegs
;;;;

(defn pegged?
  "
  [函数介绍]
  get-in 函数就是 assoc-in 的逆向函数:
  assoc-in 是往 associative structure 中添加元素;
  get-in 是从 associative structure 中获取元素;

  (assoc-in m [key1 key2 key3 ...] value)

  (get-in m [key1 key2 key3 ...])

  (assoc m key1 value1 key2 value2)

  (get m key)

  这个函数是从 board 中获取指定元素是否有楔子.

  map, int -> bool
  "
  [board pos]
  (get-in board [pos :pegged]))

(defn valid-moves
  "获取某个位置的有效跳跃位置, 并存储为一个 map 结构:
  (1)key 是目的地, 必须满足其 :pegged 为 false;
  (2)value 是中间地, 必须满足其 :pegged 为 true
  (1)(2) 条件必须同时满足

  该函数的本质, 是把 map 中的每个 key value 拿出来做一个
  逻辑过滤, 剩下的仍然存储为一个 map

  同时也可以发现, concat 和 into 两个函数其实功能类似 --- 都是融合并没有生成*嵌
  套*集合. 但 into 更偏向于集合类型的转换, 而 concat就是融合.

  map, int -> map"
  [board pos]
  (into {}
        (filter (fn [[destination neighbor]]
                  (and (not (pegged? destination))
                       (pegged? neighbor)))
                (get-in board [pos :connect]))))


(defn valid-move?
  "给定起始位置 pos 和目的位置 destination , 返回 neighbor 位置如果不存在, 则返回
  nil.

  map, int, int -> nil or int"
  [board pos destination]
  (get (valid-moves board pos) destination))

(defn remove-peg
  "将楔子从棋盘中的指定位置移除, 本质就是把 board 对应的 map 中的 pos 位置的子 map
  的 :pegged 键对应的值设置为 false

  map -> map"
  [board pos]
  (assoc-in board [pos :pegged] false))

(defn place-peg
  "在指定位置楔入楔子, 设置 :pegged 为 true

  map -> map"
  [board pos]
  (assoc-in board [pos :pegged] true))

(defn move-peg
  "把楔子从一个位置移动到另一个为置

  [function intro]
  (if-let [bindings test]
    (then)
    (else))

  About the Process Orientation Language, also we can program like
  (func-A (func-B ...)) to mock it. Because the immutable feature of
  clojure, when we handle the object by (func-B immu-obj) then we get
  another modified and immutable obj, then we can pass it to another
  function func-A.

  map, int, int -> map
  "
  [board p1 p2]
  (place-peg (remove-peg board p1) p2))

(defn make-move
  ""
  [])
