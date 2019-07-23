(ns mock-pegthing.core
  (:gen-class))

;; [Take Care]
;; [Tech Tip]

(import java.util.Date)

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

(def boardx
  {
   :rows 4
   1 {:pegged true :connect {4 2 6 3}}
   2 {:pegged true :connect {7 4 9 5}}
   3 {:pegged true :connect {8 5 10 6}}
   4 {:pegged true :connect {1 2 6 5}}
   5 {:pegged true :connect {}}
   6 {:pegged true :connect {4 5 1 3}}
   7 {:pegged true :connect {2 4 9 8}}
   8 {:pegged true :connect {3 5 10 9}}
   9 {:pegged true :connect {2 5 7 8}}
   10 {:pegged false :connect {8 9 3 6}}
   }
  )

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

;; [ 程序举例 ]无限集的使用举例
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

  (assoc-in __ [] __) 的第二个参数对于数组来说就是数组索引，但是对于 map 来说既在索引也在创建.

  当键不存在时，就一直创建
  (assoc-in {} [:pegged :hello] 1)
  => {:pegged {:hello 1}}

  当键不存在时，就一直创建, 并且不断嵌套
  (assoc-in {} [:pegged :hello :yiddi] 1)
  => {:pegged {:hello {:yiddi 1}}}

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
                (triangular? destination)) ;; 处在行尾位置
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

  board 的本质是一个 map

  由这里可以看出, max-pos 是指整个棋盘最后一个位置.
  "
  [rows]
  (let [initial-board {:rows rows}
        max-pos (row-tri rows)]
    (reduce (fn [board pos]
              (add-pos board max-pos pos))
            initial-board
            (range 1 (inc max-pos)))))

(new-board 4)

;;;;
;; Move pegs
;;;;

(defn pegged?
  "
  [Tech Tip]

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

  (3) 本条并没有在此检测，需要在调用处进行检测， pos 必须是 pegged 的

  [编成感悟]

  由于 clojure 的核心是对集合进行快速操作，而且集合的操作函数中 filter take drop
  等的存在，对集合进行条件裁减是非常简单的一件事，所以条件判断非必须的都不需要。尤其是
  对函数参数的条件判断，完全可以留给调用者在调用时通过 filter 完成。

  该函数的本质, 是把 map 中的每个 key value 拿出来做一个
  逻辑过滤, 剩下的仍然存储为一个 map

  同时也可以发现, concat 和 into 两个函数其实功能类似 --- 都是融合并没有生成*嵌
  套*集合. 但 into 更偏向于集合类型的转换, 而 concat就是融合.

  map, int -> map"
  [board pos]
  (into {}
        (filter (fn [[destination neighbor]]
                  (and (not (pegged? board destination))
                       (pegged? board neighbor)))
                (get-in board [pos :connect]))))


(defn valid-move?
  "给定起始位置 pos 和目的位置 destination , 返回 neighbor 位置如果不存在, 则返回
  nil.

  [返回nil or obj 的好处]

  因为在 clojure 中 nil 在进行 bool判断时就等于 false, obj在进行bool判断时就等于
  true，所以非常非常常用。

  [Tech Tip]

  从 map -> filter -> submap -> get -> if-let是一个实现 contains? 的相当好用的套
  路。 map 因为其可以被堪称 [[][]...] 再通过 filter 可以很方便的同时对 key value
  进行过滤。然后通过 get 这个函数的特殊返回形式 nil or obj. 可以很方便的用在
  if-let 语句中.

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

  [Tech Tip]

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

(defn my-make-move
  "把楔子从一个位置移动到另一个为置"
  [board p1 p2]
  (if-let [legally-pegged (and (contains? (get-in board [p1 :connect]) p2) ;; p2包含在p1的 connect中
                               (= true (get-in board [p1 :pegged])) ;; p1 被 peg
                               (= true (get-in board [(get-in board [p1 :connect p2]) :pegged])) ;; neighbor 被 peg
                               (= false (get-in board [p2 :pegged]))) ;; p2 没有被 peg
           ]
    (move-peg ;; 把 p1 的peg拿给p2。
      (remove-peg board (get-in board [p1 :connect p2])) ;; neighbor元素的peg拿掉
      p1 p2)
    board ;; 这个 else 部分，可以不用写，只写then的部分即可，表示如果并非如此则不做任何操作。
    ))

(defn make-move
  "进行完整的移动操作, pos 和 neighbor 的楔子拿掉，destination的楔子楔入。"
  [board p1 p2]
  (if-let [neighbor (valid-move? board p1 p2) ]
    (move-peg (remove-peg board neighbor) p1 p2)))




(defn my-1st-can-move?
  "查看某个board是否还有可以移动的楔子。

  自己实现的第一个版本使用 reduce 求出每个 row-key 对应的 :connect 的目标都不是
  valid-move 才能确定 can not move. 显然用 some 更合适。因为只是想知道有没有。"
  [board]
  (reduce (fn [row-key1 row-key2] ;; 针对所有 pos 进行判定看看是否存在可以移动的
                                  ;; pos，只要有一个 pos 可以移动，该board就是
                                  ;; can-move 的
            (or row-key1
                (reduce (fn [destination1 destination2]
                          (or destination1
                              (valid-move? board row-key2 destination2))) ;; 根据 valid-move? 对该
                                                                          ;; pos 及其 destination集合作判定
                                                                          ;; 看看该 pos是否可以移动
                        false
                        (map #(first %)
                             (get-in board [row-key2 :connect]))))) ;; 取出某个
                                                                    ;; pos的在不
                                                                    ;; 考虑
                                                                    ;; pegged状
                                                                    ;; 态时的所
                                                                    ;; 有可以移
                                                                    ;; 动的
                                                                    ;; destination
                                                                    ;; 的集合
          false
          (filter #(= true (get-in board [% :pegged])) ;; 根据所有 pos的 pegged
                                                       ;; 状态进行一次过滤，只有
                                                       ;; pegged为true的才能 move
                  (range 1 (inc (row-tri (:rows board) ;; 获取所有pos并组成集
                                                       ;; 合 [1 2 3 4 ...]
))))))


(defn my-2nd-can-move?
  "查看某个board是否还有可以移动的楔子。"
  [board]
  (some #(apply valid-move? board %) ;; 使用 valid-move? 对每一个 [pos des] 对其
                                     ;; 进行判断看是否为有效 move, 只要还有一个
                                     ;; 为true整体就是 can move的
        (reduce (fn [row-seq1 row-seq2]
                  (concat row-seq1 row-seq2)) ;; 将独立的每个 pos的集合，concat成整个的集合
                                              ;; [[pos1 des1] [pos1 des2] [pos2 des1] [pos2 des2]]
                (map (fn [row-key] ;; 针对所有 pos 塑造成 [[[pos1 des1] [pos1 des2] ...]
                                  ;;                    [[pos2 des1] [pos2 des2] ...]]
                       (map (fn [destination]
                              [row-key destination]) ;; 针对某个 pos 和其对应的
                                                     ;; destination集合组
                                                     ;; 成[ [pos des1] [pos
                                                     ;; des2]...]
                            (map #(first %)
                                 (get-in board [row-key :connect])))) ;; 针对某个 pos 获取其连接的 destination的集合
                     (range 1 (inc (row-tri (:rows board)))))))) ;; 为何要重新构
                                                                 ;; 造pos集合，
                                                                 ;; 毕竟board中
                                                                 ;; 已经有pos了，
                                                                 ;; 只是比较分散

(defn my-4th-can-move?
  ""
  [board]
  (some (complement nil?)
        (reduce (fn [cache-vect pos-map]
                  (concat cache-vect (map (fn [des-nei]
                                            (apply valid-move? board [(first pos-map) (first des-nei)]))
                                          (get (second pos-map) :connect))))
                []
                (filter #(get (second %) :pegged) board)))
)


(my-4th-can-move? boardx)

(defn my-3rd-can-move?
  "之前的两种方法都忽略了 valid-moves 这个函数，进而把 valid-moves 相当于又实现了
  一遍。 这里直接利用 valid-moves 函数来验证每一个 pos 是否还存在可以移动的
  destination。 然后通过 some 函数对pos集合进行爆破出每一个元素.

  [Tech Tip]

  comp, partial 两者是可以构造函数字面量的

  map, reduce, filter, take, some, drop 都是操作集合类型的函数，不同的是：
  map: 一一映射
  reduce: 揉和为一
  filter: 条件裁减集合
  take: 顺序裁减集合, 注意误删问题
  drop: 顺序裁减集合, 注意误删问题
  some: 条件揉和为一
  get: 验证集合内容存在
  get-in: 验证集合内容存在
  contains?: 验证集合索引存在

  但凡可以接受函数的地方，都可以使用函数字面量，也就可以接受 comp, partial

  "
  [board]
  (some ;; fn [pos] (not-empty (valid-moves board pos)))
        ;; #(not-empty (valid-moves board %))
   (comp not-empty (partial valid-moves board)) ;; 注意这里既可以提供函数字面量， 也可以
                                       ;; 提供正统函数
   (map first ;; 这里也一样，可以提供函数字面量或者函数, 该 map用来获取pegged 为
              ;; true的 pos 集合
        (filter #(get-in (second %) [:pegged]) ;; 在这里追加判断某个 pos是否可移
                                                 ;; 动的最后一步：该 pos上有peg,
                                                 ;; 一开始我想当如果想获
                                                 ;; 得 :pegged 就必须要 key，也
                                                 ;; 就是 pos，但其实没必要，因为
                                                 ;; 我忽略了 map 的本质也是 seq
                                                 ;; of two item vector, [Tech Tip] 仅仅通过
                                                 ;; first 和 second 就可以取
                                                 ;; 得 :pegged
                ;; [Take Care] (drop 1 board) ;; 原来的打算是由于 board的第一行是 :rows 4 但
                ;; 是没有考虑到 map 构建之后是不会按照原来的顺序存储的，所以
                ;; drop 会误删数据。最后是 get-in 函数拯救了我，因为不存在的情况
                ;; 下就是返回 nil，而 clojure bool判断对待nil和false是一样的.
                ;; 换言之，只用 board 即可。 get-in 真是 filter 的好帮手.
                board
                ))))

(defn can-move?
  ""
  [board]
  (some (comp not-empty (partial valid-moves board))
        (map first (filter #(get (second %) :pegged) board))))

;; todo
;; TODO不知道如何查看函数运行时间
;; TODO为何书中答案代码如此简短，我的却如此冗长---多个 reduce map 嵌套
;; DONE assoc-in 是否可以嵌套的添加元素。
;; 对于 map 数据来说毫无问题， assoc-in 经常直接用来直接创建内嵌的map
;; (assoc-in {} [1 :connect 4] 2)
;; => {1 {:connect {4 2}}}

;; DONE关于如何确定某个 pos 最终可以移动到某个位置
;; 需要两个关键点才能确定： 
;; 1) pegged，pos 必须pegged, neighbor 必须 pegged, destination 没有 pegged
;;    这个可以通过 pegged? 函数来确定.
;; 2) 位置，必须满足一定位置关系，才有可能跳跃，这个已经在 connect 函数中实现了。
;;
;; 1)+2) 就是在 valid-move 中实现的

;; 3) 初始位置，亦即 pos 的pegged必须为 truy，这个必须在调用处进行判断，完全可以
;; 通过对集合进行过滤来实现。


;; TODO 为什么 map 数据的存入顺序和最终顺序会存在差异。

;; DONE [Tech Tip] filter + get + {} 是超级经典的 'FG' 组合. 适合从全集中切割出
;; 只含有某些元素的子集。

;; DONE [Tech Tip] 想获取某种 [ [1 [3 4]] [11 [31 41]] ]=> [[1 3][1 4]] 的需要使
;; 用 map 嵌套其中 1 需要由上层map提供，而 3 4 需要单独作为下层集合提供



;;;;
;; Represent board textually and print it
;;;;


(def alpha-start (int \a))
(def alpha-end (int \z))
(def letters (map (comp str char) (range alpha-start (inc alpha-end))))
(def pos-chars 3)


(def ansi-styles
  {
   :red "[31m"
   :green "[32m"
   :blue "[34m"
   :reset "[0m"
   })

(defn ansi
  "https://www.jianshu.com/p/248a276e1a18
  有解释， \u001b 就是用来在控制台输出带颜色的字符的

  process.stderr.write('\u001b[31m error \u001b[0m)
  这一段代码会让控制台 [ 输出红色的 error ]

  前面的\u001b[31m用于设定SGR颜色，后面的\u001b[0m相当于一个封闭标签作为前面SGR
  颜色的作用范围的结束点标记。

  这种先定义一个 map，然后再定义一个函数接受 key 值的方式，提供了一种比较好的赋
  予“魔数”意义的方法。
  "
  [style]
  (str \u001b (style ansi-styles)))


(defn colorize
  "像 process.stderr.write('\u001b[31m error \u001b[0m) 一样构造整个代颜色的字符串

  1. 其中参数color应该输入一个symbol，比如 :red or :blue. 经过 ansi-styles 映射
  为具体的字符串 “[31m” or “[32m”

  2. 其中参数 text 就是要被上色的字符串，就像上面例子中的 “error”
  "
  [text color]
  (str (ansi color) text (ansi :reset)))


(defn my-render-pos
  "输入 board 和 pos 位置，根据该位置是否被 pegged 来给该位置的字母上色:
  1. 如果是 :pegged 为 true 上色 blue 并且字母前加 “0”
  2. 如果是 :pegged 为 false 上色 red 并且字母前家 “-”

  因为整个三角形都是按照字母顺序排列的"
  [board pos]
  (let [letter (nth letters pos)
        peg-bool (get-in board [pos :pegged])]
    (if peg-bool
      (colorize (str "0" letter) :blue)
      (colorize (str "-" letter) :red)
      )))

(defn render-pos
  [board pos]
  (str (nth letters (dec pos))
       (if (get-in board [pos :pegged])
         (colorize "0" :blue)
         (colorize "-" :red))))

(defn row-positions
  "给出行数，返回该行 pos 的列表"
  [row-num]
  (range (inc (or (row-tri (dec row-num)) 0))
         (inc (row-tri row-num))))


(defn row-padding
  "
  计算要在每一行前面添加多少个空格

  ____xxx____   row-num

  ...........
  xxxxxxxxxxx   rows

  因为第几行就有几个字符，用最后一行的字符减去所求那行的字符数，就等于两边的空格
  数， 然后在除以二，就是单边的空格数。
  "
  [row-num rows]

  (let [pad-length (/ (* (- rows row-num) pos-chars) 2)]
    (str (take pad-length (repeat " "))) ;; 要习惯这种通过 ”重复元素 repeat“
                                         ;; => "截取 take" => "拼接 str" 创建5个
                                         ;; 空格字符串的这种方式
    ))

(defn render-row
  "
  绘制每一行 --- 为每一行生成字符串, 包含空白字符和被上色完成的字符

  因为上色目前只有针对 pos 的函数 --- render-pos，所以需要对通过 (row-position
  row-num) 获得的行坐标集合使用map 进行映射, 然后还需要添加空格，这个可以通过
  clojure.string/join 来实现

  "
  [board row-num]
  (str (row-padding row-num (:rows board))
       (clojure.string/join " " (map (partial render-pos board) (row-positions row-num)))))

(defn print-board
  "
  [Tech Tip]

  doseq 只会返回 nil，但是却可以很好的实现 笛卡尔积集 和 映射集。所以适合用在
  side-effect方面，比如 println. 其作用相当于 python 中的 for in.

  笛卡尔积集： [1 2 3] [4 5 6] => [1 4] [1 5] [1 6]
                                  [2 4] [2 5] [2 6]
                                  [3 4] [3 5] [3 6]

  笛卡尔积集相当于两层 for 循环，外层循环 [1 2 3] 内层循环 [4 5 6]

  (doseq [x [1 2 3]
          y [4 5 6]]
     (println x y))


  映射集：[1 2 3] [4 5 6] => [1 4] [2 5] [3 6]

  (doseq [[x y] (map list [1 2 3] [4 5 6])]
     (println x y))


  [Tech Tip]

  之前一直疑惑为什么 let 没法解析出 map， 今天在这里发现了可以作的方法：

  http://clojuredocs.org/clojure.core/doseq#example-542692c6c026201cdc326924


  在解析map数据结构时不能直接使用字面量，{:size 13}, 他在 let 赋值是无法被当
  作 [[:size 13]]. 需要通过 map 进行一次转换，转换成 seq 数据，才能被let解析。

  (let [[[k v]] {:size 13}] (println k v)) ==> 报错

  (let [[[k v]] (map identity {:size 13})]) ==> :size 13


  [Tech Tip]

  注意：let 与 doseq，loop 在解析 seq 时，原理相似方法不同：
  1. let 是*整体一次*赋值
  2. doseq 是*元素循环*赋值.

  (let [x [1 2 3]] (println x))  equals to  (println [1 2 3])

  (doseq [x [1 2 3]] (println x))  equals to (do (println 1) (println 2) (println 3))

  map -> nil
  "
  [board]
  (doseq [row-num (range 1 (inc (:rows board)))]
    (println (render-row board row-num))))


;;;;
;; Interaction
;;;;
;;                                          +---------------------------------------------------------+
;;                                          |                                                         |
;;                                          |                   +--------- can-move? N ---> game-over +
;;                                          v                   |                                     |___ END
;; prompt-rows => prompt-empty-peg => prompt-move ---- successful-move --- can-move? Y ---+
;;                                          ^_____________________________________________|


;; [Tech Tip]

;; int 转换 char : (char 97)

;; char 转换 int : (int \a)

;; char 转换 string : (str \a)

;; string 转换 char : (first \"a\")

;; 注意，在 java 中string是可以被看成数组的，那么当索引一个数组的某个元素时就是
;; char

(defn letter->pos
  "
  把用户输入的字母，转换成对应的位置（数字）

  目前已经有的数据是： letters = [\"a\", \"b\", ..., \"z\"]

  用户的输入会是一个字符串，eg \"x\"

  string -> int

  "
  [letter]
  (inc (- (int (first letter)) alpha-start))
  )


(defn get-input
  "
  [Tech Tip]

  1.

  clojure.string/trim 将字符串的空格去掉

  (clojure.string/trim \" hello \") => hello

  2.

  clojure.string/lower-case 将字符串转换成小写

  (clojure.string/lower-case \"HELLO\") => hello

  3.

  read-line 可以从终端读取文本, 不同于 java or python 的终端输入函数，
  (read-line) 不允许接提示文本作为参数

  4.

  set 可以直接作为函数来使用，接入一个参数，如果该参数在集合中则返回该参数，如果
  不在则返回 nil. 怪不得 set 的符号是这个 #{} ，很形象的表示出他是个函数。

  (#{1} 2) => nil
  (#{1} 1) => 1

  关于 seq 直接作为函数使用：

  [N] list as function

  ('(1 2 3) 1) => error

  [Y] vector as function

  ([1 2 3] 1) => (nth [1 2 3] 1) => 2

  [Y] map as function

  ({:size 13 :age 24} :age) => (get {:size 13 :age 24} :age) => 24

  [Y] set as function

  (#{1 2 3} 1) => (get #{1 2 3} 1) => 1

  "
  ([] (get-input ""))
  ([default]
   (let [input-str (clojure.string/trim (read-line))]
     (if (empty? input-str)
       default
       (clojure.string/lower-case input-str)))))

(defn my-characters-as-strings
  "
  将字符串转换成字符数组

  我的这种做法并没有考虑到，输入 \"a0\" 时，我希望获得的输出时 \"a\" 而不是
  \"a\" \"0\".

  [Tech Tip]

  (clojure.string/split 字符串 正则表达式)

  一定注意字符串是第一个参数。


  "
  [string]
  (map first (clojure.string/split string #"")))


(defn characters-as-strings
  "
  [Tech Tip]

  如果把字符串看成一个数组，re-seq 的工作有点像是 filter, 不同的是：

  1. filter, filter by bool expression.

  (filter (partial > 3) [1 2 3 4]) => [4]

  2. re-seq, filter by regular expression.

  (re-seq #\"[a-zA-Z]\" \"a0b0c0\") => [\"a\", \"b\", \"c\"]

  "
  [string]
  (clojure.core/re-seq #"[a-zA-Z]" string))


(defn prompt-move
  "
  该函数按顺序作如下4件事情：
  1. 打印当前棋盘，
  2. 输入两个位置(起点终点)，比如 “a0b0” ，
  3. 更改棋盘map数据结构 ，
  4. 根据新的map数据结构，打印新的棋盘

  "
  [board]

  (println "\nHere's your board:")
  (print-board board)
  (println "Move from where to where? Enter two letters:")
  (let [input (map letter->pos (characters-as-strings (get-input)))]
    (if-let [new-board (make-move board (first input) (second input))]
      (successful-move new-board) ;; 这是函数的一个出口，他会做一个判断---棋盘是
                                  ;; 否终结状态，如果不是，继续循环；如果是，停止
      (do
        (println "\n!!! That was an invalid move :(")
        (prompt-move board) ;; 这是函数的另一个出口，他不断调用自己，形成无限循环
        )))
  )

(defn successful-move
  "
  1. 判断棋盘是否终结态，即无子可移
  2. 如果不是，继续调用 Prompt-move 提示用户移动，
  3. 如果是，则调用 game-over 函数
  "
  [board]
  (if (can-move? board) ;; 判断棋盘是否终结态
    (prompt-move board)
    (game-over board)))

(defn game-over
  "
  1. 打印当前棋盘
  2. 提示玩家游戏结束，以及剩下的 peg 为 true 的棋子数量（数量越少越胜利）
  3. 提示用户是否继续尝试
  4. 如果用户输入 y，调用 prompt-rows 让用户输入起点和终点
  5. 如果用户输入 x，结束整个程序.

  "
  [board]
  (let [remaining-pegs (count (filter :pegged (vals board)))]
    (println "Game Over! you had" remaining-pegs "pegs left:")
    (print-board board)
    (println "Play again? y/n [Y]")
    (let [input (get-input "y")]
      (if (= "y" input)
        (prompt-rows)
        (do
          (println "Bye~!")
          (System/exit 0))))
    ))

(defn prompt-empty-peg
  "
  1. 打印出整个棋盘
  2. 提示用户输入移除哪个位置的 peg，pegged 置为 false
  3. 调用 prompt-move , 让用户输入两个字母 --- 从哪个位置跳跃到哪个位置
  "
  [board]
  (println "here's your board:")
  (print-board board)
  (println "remove which peg? [e]")
  (prompt-move (remove-peg board (letter->pos (get-input "e")))))

(defn prompt-rows
  "

  1. 提示用户输入行数---一个数字, 默认情况下是 5。

  2. 根据用户在终端中输入的行数，创建棋盘的数据结构

  3. 调用 prompt-empty-peg 函数，要求用户输入一个标识位置的数字，并移除
  其 peg --- 将 :pegged 由 true 设置为 false

  "
  []
  (println "How many rows? [5]")
  (let [rows (Integer. (get-input 5))
        board (new-board rows)]
    (prompt-empty-peg board)))

(defn -main
  [& args]
  (println "Get ready to play peg thing!")
  (prompt-rows))
