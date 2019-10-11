(ns the-divine-cheese-code.visualization.svg
  (:require [clojure.string :as s])
  ;;[编程感想:运算符重载第一步, 剔除旧的]
  ;;
  ;; 想实现导入库中的某些函数的功能，该如何处理，这里给出了经典的解决办法
  ;; 这个特别类似 C++ 中的运算符重载以及 Java 中的函数多态.
  ;;
  (:refer-clojure :exclude [min max]))




(defn test-dri
  [exp act]
  (condp = (= exp act)
    true "PASS"
    false (str "FAILED, the expect is: " exp ", the actual is: " act)
    ))



;; 什么是框架，我觉得这个就有点像是框架了，在 oo 语言中你可以清楚的了解某个函数
;; 的输入和输出是什么。但是这里你就没那么清晰，只是大概知道传递进来的是一个函数
;; （功能），他出现在什么位置.

;;                                 +-+                     +-+-+-+-+
;;                                 | |                     | | | | |
;;                                 +-+                     +-+-+-+-+
;;                                  |                       |
;;              +-------------------------------------------+
;;              |                   |    作为函数(:lat)
;;              |                   |
;;              |                   |
;;              |                   |作为函数(max)
;;  未来        |                   |
;;  作为集合    v                   v
;;  +-+-+-+     |  +-+-+-+          |
;;  | | | |----->--| | | |---------------------------+
;;  +-+-+-+        +-+-+-+                           |
;;                                                   v
;;                                 /   +-+-+-+-+    +-+    +-+-+-+-+   \
;;                          zip   |    | | | | |    | | => | | | | |    |
;;                                 \   +-+-+-+-+    +-+    +-+-+-+-+   /

;; 你只能大概总结出一个这样的图，但因为该函数的参数是函数（功能），功能不确定，
;; 整个函数就是一个框架。

(defn comparator-over-maps
  "

  [编程感想：加入参数的目的是，暂时不确定留待其他函数思考提供]

  比如这里的 ks 应该是 :lat :lng 这些 map keys，但是我暂时不知道该怎么从 map 结
  构中提取全部 keys，可以将其提为参数. 留待其他函数提供。

  "
  [comparison-fn ks]
  (fn [maps]
    (zipmap ks
            (map (fn [k] (apply comparison-fn (map k maps))) ks))))


(def heists [{:location "xxx"
              :name "xxx"
              :lat 23.12
              :lng 87.14}
             {:location "xxx"
              :name "xxx"
              :lat 82.11
              :lng 76.89}])

(defn my-comparator-over-maps3
  [func keys]
  (fn [mps]
    (zipmap keys
            (map (fn [k] (apply func (map (fn [mp] (k mp)) mps))) keys))))

(def my-max (my-comparator-over-maps3 clojure.core/max [:lat :lng]))
(def my-min (my-comparator-over-maps3 clojure.core/min [:lat :lng]))

(my-max heists) 
;; => {:lat 82.11, :lng 87.14}
(my-min heists) 
;; => {:lat 23.12, :lng 76.89}


;; [编程感想：运算符重载第二步, 通过 def 以新代旧]
(def min (comparator-over-maps clojure.core/min [:lat :lng]))
(def max (comparator-over-maps clojure.core/max [:lat :lng]))

(min [{:lat 1 :lng 3} {:lat 5 :lng 0}]) ;; => {:lat 1 :lng 0}


(defn my-comparator-over-maps5
  "

  [编程感想： 关于函数导入中的 . 和 /]

  1. / 之后接的是[ 函数名 ]
  2. 最后一个 . 之后是[ 文件名 ], 之前的全部是[ 文件夹 ]

  比如 clojure.core/max 就是 clojure 文件夹下的 core.clj 下的 max 函数。


  [编程感想： merge/merge-with 的缺点]

  大部分时候处理 map 结构间的运算都可以依赖 merge-with， 这有个前提---[ 每个 key
  的value的类型一致 ], 比如这个函数遇到的问题，就是如此，当你使用
  (merge-with - map1 map2) 的时候，也就是说 map 的每个 value 都必须可以被 “减”。

  merge 无法处理的还有诸如：
  1. 我只想处理其中的几个 key， 而不是所有 key。
  2. 我的 map 的 key 类型不一致。

  这个时候考虑使用 (zipmap partial_ks (map (map f ks) mps)) 来处理这些问题。

  [ zipmap + embedded_map ] is a good complement for [ merge and merge-with ]

  "
  [func]
  (fn [mps]
    (apply merge-with func mps)))
((my-comparator-over-maps5 clojure.core/max) heists)


(defn my-max
  "
  1. get all keys of maps

  [:lat :lng :llg]

  2. get the max value of every index of vector by reduce

  {:lat 1 :lng 3 :llg 4}  => [ [1 3 4] --+--> [[1 5] [3 0] [4 2]] --> [5 3 4] --+--> [[5 9] [3 6] [4 7]] --> [9 6 7]
  {:lat 5 :lng 0 :llg 2}  =>   [5 0 2] --+                                      |
  {:lat 9 :lng 6 :llg 7}       [9 6 7] ] ---------------------------------------+

  这里我采用的就是完全的横向方法, 接下来的函数，我解释了如何使用纵向与横向两种方
  法
  "
  [maps]
  (let [keys (map #(first %) (first maps)) ]
    (into {} (map vector ;;map list will lead an ERROR
                  ;; 注意： list of list 是不能直接转换成 map 的，但是 vector of
                  ;; vector 可以。所以，如果这里换成 list 就会报错。
                  keys
                  (reduce (fn [v1 v2] (map (partial apply clojure.core/max) (map list v1 v2)))
                          (map (fn [m] (map #(second %) m)) maps))))))

(test-dri {:lat 5 :lng 3 :llg 4} (my-max [{:lat 1 :lng 3 :llg 4} {:lat 5 :lng 0 :llg 2}]))


;; 经过对上面这个函数进行提取参数，可以导出与书中函数非常相似的一个【横向】函数：


(defn my-comparator-over-maps1
  [maps func ks]
  (into {} (map vector
                ks
                (reduce (fn [v1 v2] (map (partial apply func) (map list v1 v2)))
                        (map (fn [m] (map #(second %) m)) maps)))))

;; 进一步对其做延伸思考，得出【纵/横】向两个函数的写法规律。他们都与嵌套 map 的顺
;; 序有关。

(defn my-comparator-over-maps2
  [mps func ks]
  ;; [编程感想: 双 map 嵌套实现笛卡尔积集式组合,可纵向可横向]
  ;;
  ;; 注意 (1) map 是如何实现 catisian product 式交叉的
  ;;      (2) map 参数的前后顺序对最终结果的影响，是纵向还是横向。
  ;;
  ;; 一， ks 内，mps 外
  ;; (map (fn [mp] (map (fn [k] (k mp)) ks)) mps)
  ;;
  ;; {:lat 1 :lng 3 :llg 4}  => [ [1 3 4]
  ;; {:lat 5 :lng 0 :llg 2}  =>   [5 0 2]
  ;; {:lat 9 :lng 6 :llg 7}       [9 6 7] ]
  ;;
  ;; 由于 keys 在内，mps 在外，所以会先便利所有的 key 然后按照 map 组合, 所
  ;; 以遍历 key 的结果在内层，map 组合的形式展示在外层
  ;;
  ;; 二， mps 内，ks 外
  ;; (map (fn [k] (map (fn [mp] (k mp)) mps)) ks)
  ;;
  ;; {:lat 1 :lng 3 :llg 4}
  ;; {:lat 5 :lng 0 :llg 2}
  ;; {:lat 9 :lng 6 :llg 7}
  ;;       |      |      |
  ;;       v      v      v
  ;; [ [1 5 9] [3 0 6] [4 2 7] ]
  ;;
  ;; 由于 ks 在外，mps 在内，所以会先便利所有的 map 然后按照 key 组合, 所
  ;; 以遍历 map 的结果在内层，key 组合的形式展示在外层

  ;; [编程感想： zipmap]
  ;;
  ;; (zipmap vector-1 vector-2) 的意思就是先对两个 vector 进行 zip，然后将其转换
  ;; 成 map。

  (zipmap ks (map (fn [k] (apply func (map (fn [mp] (k mp)) mps))) ks))
  )


;; test my functions
(def locas [
           {:lat 1 :lng 3 :llg 4}
           {:lat 5 :lng 0 :llg 2}
           {:lat 9 :lng 6 :llg 7}
           ])
(test-dri {:lat 9 :lng 6 :llg 7}  (my-comparator-over-maps2 locas
                                                            clojure.core/max
                                                            [:lat :lng :llg]))


;; (my-max heists) 
;; => {:lat 82.11, :lng 87.14}
;; (my-min heists) 
;; => {:lat 23.12, :lng 76.89}


(defn my-computation-over-maps
  [func ks]
  (fn [mp1 mp2] (zipmap ks
                        (map (fn [k] (apply func ;; 这里是 (apply - [1 2]) 的意思
                                            (map (fn [mp]
                                                   (let [cache-int (k mp)]
                                                     (if (or (nil? cache-int) ((complement number?) cache-int))
                                                       0
                                                       cache-int))) ;; 这里是为
                                                                    ;; 了控制那
                                                                    ;; 些为 nil
                                                                    ;; 以及非
                                                                    ;; int 类型
                                                                    ;; 的值的默
                                                                    ;; 认值为 0
                                                 [mp1 mp2])))
                             ks))))

(def minus-map (my-computation-over-maps - [:lat :lng]))

(defn my-translate-to-00
  [locations]
  (let [base (my-min locations)]
    (map (fn [lc] (minus-map lc base)) locations)))




(my-translate-to-00 heists)

(defn translate-to-00
  "

  maps 中的所有 map 与各键最小值 map 之差

  [编程感想： merge-with 和 merge]

  一， merge 的格式为：

  (merge [& maps])

  意思是用右边的 map 不断合并到左边的 map 中，最终返回左边的 map.

  二， merge-with 的格式为：

  (merge-with [f & maps])

  意思是用右边的 map 不断合并到左边的 map 中，通过 (f left right)

  重点：
  1. merge-with 用来处理 map 与 map 之间的运算相当好用。
  2. 如果没法使用 merge-with

  "
  [locations]
  (let [mincoords (min locations);; 又见运算符重载的好处，简单
        ]
    (map #(merge-with - % mincoords) locations)))

(translate-to-00 [{:lat 1 :lng 8} {:lat 7 :lng 2}])



;; 该绘图工程的整体思路:
;; 1. 找到 (0,0)，变绝对纬度为相对纬度
;; 2. 计算像素密度，将转换后经纬度坐标铆钉到画布上


;;    .          |
;;    .          |         *'
;;    *'         |
;;    .          |
;;    .          |
;;    .          |
;;    .          |
;; ..............|.......* (8 -7)......>
;;    .          |
;; --------------|-------------->
;;    .          |
;;    .          |
;;    *          |
;; (-9 8)        |         * (10 10)
;;    .          |
;;    v          v



(defn scale
  "

  这里就是实现 zoom-in 功能

  "
  [width height locations]
  (let [maxcoords (max locations);; 又见运算符重载的好处，简单
        ratio {:lat (/ height (:lat maxcoords))  ;; 如果填满整个绘图框，每个经度占多少个 pixel
               :lng (/ width (:lng maxcoords))}] ;; 如果填满整个绘图框，每个纬度占多少个 pixel
    (map #(merge-with * % ratio) locations)))    ;; 每个位置的纬度和经度对应的 pixel 应该是多少




;; 经纬度坐标放缩到画布坐标
(defn my-scale
  "5 5 [{:lat 1 :lng 2} {:lat -1 lng 3}] => [{:lat 5 :lng 0} {:lat 0 :lng 5}]"
  [cw ch locations]
  (let [maxcoords (max locations)
        ratio {:lat (/ cw (:lat maxcoords))
               :lng (/ ch (:lng maxcoords))}]
    (map #(merge-with * ratio %) locations)))
(test-dri [{:lat 5 :lng 0} {:lat 0 :lng 5}] (my-scale 5 5 [{:lat 1 :lng 2} {:lat -1 :lng 3}]))
(test-dri [{:lat 5 :lng 0} {:lat 0 :lng 5}] (scale 5 5 [{:lat 1 :lng 2} {:lat -1 :lng 3}]))


(scale 50 50 locas)
;; [{:lat 1, :lng 3, :llg 4} {:lat 5, :lng 0, :llg 2} {:lat 9, :lng 6, :llg 7}]
;; => ({:lat 50/9, :lng 25N, :llg 4}
;;     {:lat 250/9, :lng 0N, :llg 2}
;;     {:lat 50N, :lng 50N, :llg 7})



(defn latlng->point
  "convert lat/lng map to comma-separated string"
  [latlng]
  (str (:lat latlng) "," (:lng latlng))
  )
(latlng->point {:lat 43 :lng 32}) ;; "43,32"



;; [ 编程感想 ： join, split, re-seq]
;; join 是 seq -> string
;; split 是 string -> seq （去除剩下为数组）
;; re-seq 是 string -> seq （捕获为数组）

;; 将经纬度值转成字符串
(defn my-latlng->point
  "{:lat 1 :lng 4} => '1,4'"
  [location]
  (clojure.string/join "," (vals location)))
(test-dri "1,4" (my-latlng->point {:lat 1 :lng 4}))





;; 此函数用来生成线段始末点.
(defn points
  "given a seq of lat/lng maps, return string of points joined by space"
  [locations]
  (s/join " " (map latlng->point locations)))
(points locas)
;; => "1,3 5,0 9,6"
;; [{:lat 1, :lng 3, :llg 4} {:lat 5, :lng 0, :llg 2} {:lat 9, :lng 6, :llg 7}]



;; 此函数用来生成线段始末点.
(defn my-points
  "[{:lat 1 :lng 3} {:lat 5 :lng 9}] => '1,3 5,9'"
  [locations]
  (clojure.string/join " " (map #(my-latlng->point %) locations)))
(test-dri "1,3 5,9" (my-points [{:lat 1 :lng 3} {:lat 5 :lng 9}]))





(defn line
  [points]
  (str "<polyline points=\"" points "\" />"))
(line "1,3 5,0 9,6")



(defn transform
  "Just chains other functions

  [编程感想：-> 和 ->>]

  一， ->

  (-> x & forms)

  这个符号在 clojure doc 中叫做 thread to 2nd item。

  意思是把 -> 的第一个参数计算结果作为
  第二个 form 的 [第二个] 位置，计算结果作为
  第三个 form 的 [第二个] 位置，计算结果作为
  第四个 form 的 [第二个] 位置，计算结果作为
  。。。

               form 的第一个位置 form 的第二个位置
                      |             |
                      v             v
          +-------------------------+     (scale ___ width height)
          |                         v   ____________________
  (->  locations    translate-to-00     (scale width height))
                    -----------------         ^
                                              |
              (translate-to-00 locations) ----+



  二，->>

  (->> x & forms)

  这个符号在 clojure doc 中叫做 thread to last item。

  意思是把 ->> 的第一个参数计算结果作为
  第二个 form 的 [最后一个] 位置，计算结果作为
  第三个 form 的 [最后一个] 位置，计算结果作为
  第四个 form 的 [最后一个] 位置，计算结果作为

               form 的第一个位置 form 的第二个位置
                      |             |
                      v             v
          +-------------------------+     (scale width height ___)
          |                         v   ____________________
  (->> locations    translate-to-00     (scale width height ))
                    -----------------                      ^
                                                           |
              (translate-to-00 locations) -----------------+

  "
  [width height locations]
  (->> locations
       translate-to-00
       (scale width height)))

(defn my-transform
  "5 5 [{:lat 1 :lng 2} {:lat 3 :lng 0}] => [ {:lat 0 :lng 5} {:lat 5 :lng 0} ]"
  [cw ch locations]
  (->> locations
       my-translate-to-00
       (scale cw ch)))


(defn xml
  "

  svg 'template', which also flips the coordinate system

  "
  [width height locations]
  (str "<svg height=\"" height "\"width=\"" width "\">"
       ;; these two <g> tags change the coordinate system so that
       ;; 0,0 is in the lower-left corner, instead of SVG's default
       ;; upper-left corner
       "<g tansform=\"translate(0," height ")\">"
       "<g tansform=\"rotate(-90)\">"
       (-> (transform width height locations)
           points
           line)
       "</g></g>"
       "</svg>"))
