(ns my-pegthing.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;; 写这个程序遇到的问题:

;; 函数太多太混乱, 相同的功能模块没法像 OO 语言那样归结成一个模块(类)

;; 似乎有点浪费资源, 因为对不可变数据结构的任何更新都必须重建一整个结构

;; ===========

;; 我是使用每层一个 map 来表示整个棋盘的状态

;; 现在目前的工作停在: 应该在 pan-data-map 的基础上增加另外两个 vector 用来跟踪
;; 整个棋盘的 hole, movable 状态. 因为我目前就差最后两步: 第一步是如何判断整个棋
;; 盘已经无法更新 --- 到达终结态, 第二步也是最后一步就是设计一个无限循环函数, 不
;; 断读入用户输入,直到整个棋盘无路可走. 第二步大概想了一下, 应该是下面这样的代
;; 码:

;; (loop [(read-line);; 这一步是用户输入跳棋的 source and target 元素
;;        ]
;;   (if (or (pan-finished pan) (= "q" (read-line));; 判断用户是否输入 quit 或者 棋盘结束)
;;     nil
;;     (recur ...)))



;; ================ HELPER
(defn build-seq-with-same-item
  "一个 helper function: 输入指定的字符串,以及个数, 用来生成由指定个数的字符串拼接
  而成的字符串"
  [number str-item]
  (reduce (fn [i1 i2] (str i1 ((fn [item] str-item) i2))) "" (range number)))

(def bool-peg-state-map
  "一个 helper definition: 用来根据 :peg-exist 映射绘制棋盘时的字符, 如果该位置是空(hole, false) 则绘制 -,
  如果是 peg (true) 则绘制 0"
  {true "0"
   false "-"})

(defn get-line-and-index
  "一个 helper function: 用来根据输入的某个字母的 unicode 数值,
  返回这个数值应该在整个三角形的什么位置: [layer, index], 注意返回的 layer 层数
  是从 0 开始, 这一点跟 pan-data 中的定义不一样"
  [alpha-unicode]
  (loop [n 1
         res (inc (- alpha-unicode 97))
         l 0]
    (if (<= res 0)
      [(dec l) (+ res (dec l))]
      (recur (inc n)
             (- res n)
             (inc l))))
  )

(defn update-map
  "一个 helper function: 用来查找 map 数据结构中的指定 key 值, 匹配到则用 value 进
  行更新, 用来对 initial-pan-data 进行更新"
  [map-data k v]
  (into {} (map (fn [k-v-vect]
                  (if (= (first k-v-vect) k)
                    [k v]
                    k-v-vect))
                map-data)))
;; ================ <<<<<<

(defn build-pegs
  ;; 根据三角形层数 input-layer-num, 构造棋盘数据结构
  (
   [input-layer-num]
   (map (fn
          [num1 num2] ;num1=[0~4] num2=[1~5]
          {:front-space (- input-layer-num num2)
           :layer (dec num2); make it range start from 0
           :movable (map (fn [item] false) (range num2)) 
           :inner-idx (range num2)
           :peg-exist (map #(>= % 0) (range num2))
           :char-unicode (range (+ 97 (/ (+ (* num1 num1) num1) 2))
                                (+ 97 (/ (+ (* num2 num2) num2) 2)))})
        (range 0 input-layer-num)
        (range 1 (+ 1 input-layer-num)))
   ))

(def initial-pan-data (build-pegs 5)) 

;; -------------- test initial-pan-data ------------------
initial-pan-data 
;; -------------- test initial-pan-data ------------------

(defn convert-pan-data-to-well-graphic-string
  "根据 pan-data 绘制整个棋盘, 生成棋盘 string"
  [pan]
  (reduce (fn
            [line1 line2]
            (clojure.string/join "\n" [line1 line2]))
          (map (fn
                 [line-map]
                 (str (build-seq-with-same-item (:front-space line-map) "  ")
                      (clojure.string/join "  " (map #(str (char %1) (get bool-peg-state-map %2))
                                                     (:char-unicode line-map)
                                                     (:peg-exist line-map)))))
               pan)))

;; ------------test:draw-pan, begin--------------
(print (convert-pan-data-to-well-graphic-string initial-pan-data))
;; ------------test:draw-pan, end--------------


(defn update-peg-exist-by-specify-initial-hole-letter
  "选择第一个 hole 的位置, 据此找到该位置在 pan-data 中的标志位 :peg-exist, 将其置
  为 false"
  [letter pan]
  (let [[ly idx] (get-line-and-index (int letter))]
    (update-peg-exist-by-hole-location [ly idx] pan)
  ))


;; 这里需要注意, 因为该函数是针对单个坐标的改变, 更新整个 pan, 如果同时有三个改变呢,
;; 这三个改变要串联起来, 通过 pan 参数进行传递, 也许效率有问题,需要改进
(defn update-peg-exist-by-hole-location
  "输入被更新位置的 layer 以及 index, 获得新的 pan-exist 布尔 seq, 用来更新整个
  pan-data"
  [[ ly idx ] pan]
  (map (fn  ;; 这里使用 map 函数来实现对不可变集合数据结构的 update
         [layer-map-data]
         (if (= ly (:layer layer-map-data))
           (update-map layer-map-data
                       :peg-exist
                       (map (fn ;; 这里也是
                              [peg-exist-bool seq-idx] ;; 先将原值保存, 如果索引
                                                       ;; 没有匹配, 则保持原值不
                                                       ;; 变, 如果匹配上则置反.
                              (if (= seq-idx idx)
                                (do (println "\nly =" ly " ; idx =" idx )
                                    (println "peg before reverse: " peg-exist-bool)
                                    (not peg-exist-bool))
                                peg-exist-bool))
                            (:peg-exist layer-map-data)
                            (:inner-idx layer-map-data)))
           layer-map-data))
       pan))


(defn get-six-candidate-movable-by-hole-location
  "由于不知道 clojure 的 cartesian product 的 api 函数是什么, 于是我通过 3 个 map
  函数自己实现了一个, 不同的是采用了之前 clojure for the brave and truth 第三章讲解的
  把函数作为 map 的集合参数.
  对于某个 hole 的坐标, 他会产生 6 个待选的可移动位置:
  layer number: +2 -2 0;
  idx number: +2 -2;
  于是要组出 3*2 种组合"
  [ [ ly idx  ]]
  ;; (reduce (fn [item-a item-b]
  ;;           (concat item-a item-b))
  ;;         []
  ;;         (map (fn [ly-func]
  ;;                (map (fn [idx-item]
  ;;                       [(ly-func ly) idx-item])
  ;;                     (map (fn [idx-func]
  ;;                            (idx-func idx))
  ;;                          [#(+ % 2) #(- % 2)])))
  ;;              [#(+ % 2) #(- % 2) identity])
  ;;         )
  ;; 之前的理解有误, (-2-2) (-2-0) 和 (+2+2) (+2+0) 的关系. 并非都是 (+2+2) (+2+0)
  (map (fn [[ly-func idx-func]]
         [ (ly-func ly) (idx-func idx) ])
       (let [plus2 #(+ % 2)
             minus2 #(- % 2)]
         [[minus2 minus2]
          [minus2 identity]
          [identity minus2]
          [identity plus2]
          [plus2 identity]
          [plus2 plus2]]
         )
       )
  )

;; --------------- test get-six-candidate-movable-location
(get-six-candidate-movable-by-hole-location [4 2]) 
;; => ([2 0] [2 2] [4 0] [4 4] [6 2] [6 4]);;
;; --------------- test get-six-candidate-movable-location

(defn helper-movable-check
  "检测单个坐标点是否符合 2 个前提:
  (1) ly 和 idx 都是 >= 0;
  (2) layer-num >= idx

  该函数返回这个位置是否可以 movable 的第四个条件, 返回值为 true of false"
  [[ ly idx ] pan]
  (let [ layer-num (:layer (nth pan ly))]
    (and (and (>= ly 0) (>= idx 0))
         (>= layer-num idx)))
  )

(defn can-peg-movable
  "对某个 :movable 的数组中的 bool 位是否可以从 t 置 f 进行更新, 某个位置的
  movable 可以从 false -> true 必须同时满足 4 个条件:

  (1) 这个源头位置的 peg 必须是存在的
  (2) 这个目标位置的 peg 必须是 [不] 存在的
  (3) 这个位置与目标 hole 的中间位置坐标必须都是整数
  (4) 这个位置与目标 hole 的中间位置的 peg 必须存在
  (5) 必须满足 movable-check

  该函数返回这个位置是否可以最终设置为 movable 数组对应位置为 true
  "
  [[ly-from idx-from] [ly-to idx-to] pan]
  (let [intermedia-ly (/ (+ ly-from ly-to) 2)
        intermedia-idx (/ (+ idx-from idx-to) 2)
        from-peg-exist (nth (:peg-exist (nth pan ly-from)) idx-from) ;; 该位置的 peg-exist 为 true
        to-peg-is-hole (not (nth (:peg-exist (nth pan ly-to)) idx-to)) ;; 该位置的 peg-exist 为 true
        inter-location-int (and (int? intermedia-ly) (int? intermedia-idx)) ;; 中间元素的坐标为整数
        inter-peg-exist (nth (:peg-exist (nth pan intermedia-ly)) intermedia-idx)
        can-move (helper-movable-check [ ly-from idx-from ] pan)
        tip-function #(if %1
                        %1
                        (println %2))]
    (and (tip-function from-peg-exist     "ERROR: source peg does not exist")  
         (tip-function to-peg-is-hole     "ERROR: target peg is not a hole")  
         (tip-function inter-location-int "ERROR: intermedia location is not legal")  
         (tip-function inter-peg-exist    "ERROR: intermedia peg does not exist")  
         (tip-function can-move           "ERROR: movable is illegal")  
         true
         )
    )
  )

;; 这里需要注意, 因为该函数是针对单个坐标的改变, 更新整个 pan, 如果同时有三个改变呢,
;; 这三个改变要串联起来, 通过 pan 参数进行传递, 也许效率有问题,需要改进
(defn update-movable-by-specify-source-and-target-location
  "原理同 update-all-peg, 不同的是只需要更新 candidate movable change line map"
  [[ [ ly-from idx-from ] [ly-to idx-to] ] pan]
  (map (fn  ;; 这里使用 map 函数来实现对不可变集合数据结构的 update
         [layer-map-data]
         (if (= ly-from (:layer layer-map-data))
           (update-map layer-map-data
                       :movable
                       (map (fn ;; 这里也是
                              [move-bool seq-idx] ;; 先将原值保存, 如果索引
                                                  ;; 没有匹配, 则保持原值不
                                                  ;; 变, 如果匹配上则置反.
                              (if (= seq-idx idx-from)
                                (can-peg-movable [ ly-from idx-from ] [ly-to idx-to] pan)
                                move-bool))
                            (:movable layer-map-data)
                            (:inner-idx layer-map-data)))
           layer-map-data))
       pan)
  )
;; --------------test update-all-movable
(update-movable-by-specify-source-and-target-location [ [4 2] [0 0] ] initial-pan-data)  
;; => ({:inner-idx (0),
;;      :front-space 4,
;;      :char-unicode (97),
;;      :layer 0,
;;      :peg-exist (true),
;;      :movable (false)}
;;     {:inner-idx (0 1),
;;      :front-space 3,
;;      :char-unicode (98 99),
;;      :layer 1,
;;      :peg-exist (true true),
;;      :movable (false false)}
;;     {:inner-idx (0 1 2),
;;      :front-space 2,
;;      :char-unicode (100 101 102),
;;      :layer 2,
;;      :peg-exist (true true true),
;;      :movable (false false false)}
;;     {:inner-idx (0 1 2 3),
;;      :front-space 1,
;;      :char-unicode (103 104 105 106),
;;      :layer 3,
;;      :peg-exist (true true true true),
;;      :movable (false false false false)}
;;     {:inner-idx (0 1 2 3 4),
;;      :front-space 0,
;;      :char-unicode (107 108 109 110 111),
;;      :layer 4,
;;      :peg-exist (true true true true true),
;;      :movable (false false true false false)})


(defn update-six-movable-by-specify-hole-location
  " 6 个 candidate 位置点生成的 pan data 必须进行串联
  返回新的 pan-data"
  [hole-location init-pan]
  (reduce (fn [pan from-to-loc]
            (update-movable-by-specify-source-and-target-location from-to-loc pan))
          init-pan
          (map vector ;; 把 from 坐标 与 to 坐标组成一个 vector 作为
                      ;; update-one-movable-by-specify-layer-and-index 的参数
               (get-six-candidate-movable-by-hole-location hole-location)
               (repeat 6 hole-location))
          )
  )


(defn update-three-peg-when-can-movable
  "输入初始和终止 peg 字母位置, 函数检查是否可跳转, 如果不可以则不改变 pan-data 并
  提示错误, 如果可以则更新相关的 3 个 peg 的update-peg-exist-by-hole-location
  "
  [[ [ ly-from idx-from ] [ly-to idx-to] ] initial-pan]
  (if (can-peg-movable  [ ly-from idx-from ] [ly-to idx-to]  initial-pan)
    (reduce (fn [pan location]
              (update-peg-exist-by-hole-location location pan)
              )
            initial-pan
            [[ ly-from idx-from ]
             [(/ (+ ly-from ly-to) 2) (/ (+ idx-to idx-from) 2)]
             [ly-to idx-to] ])
    initial-pan))

(update-three-peg-when-can-movable [[4 1] [2 1]] (update-peg-exist-by-specify-initial-hole-letter \e initial-pan-data)) 
;; => ({:inner-idx (0),
;;      :front-space 4,
;;      :char-unicode (97),
;;      :layer 0,
;;      :peg-exist (true),
;;      :movable (false)}
;;     {:inner-idx (0 1),
;;      :front-space 3,
;;      :char-unicode (98 99),
;;      :layer 1,
;;      :peg-exist (true true),
;;      :movable (false false)}
;;     {:inner-idx (0 1 2),
;;      :front-space 2,
;;      :char-unicode (100 101 102),
;;      :layer 2,
;;      :peg-exist (truely= 2  ; idx= 1
;;    peg before reverse:  false
;;     true true),
;;      :movable (false false false)}
;;     {:inner-idx (0 1 2 3),
;;      :front-space 1,
;;      :char-unicode (103 104 105 106),
;;      :layer 3,
;;      :peg-exist (truely= 3  ; idx= 1
;;    peg before reverse:  true
;;     false true true),
;;      :movable (false false false false)}
;;     {:inner-idx (0 1 2 3 4),
;;      :front-space 0,
;;      :char-unicode (107 108 109 110 111),
;;      :layer 4,
;;      :peg-exist (truely= 4  ; idx= 1
;;    peg before reverse:  true
;;     false true true true),
;;      :movable (false false false false false)})
