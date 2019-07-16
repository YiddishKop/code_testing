(ns fwpd.core
  (:gen-class))

;; you can access the file and get the content in string format by function
;; (slurp filename)
(def filename "suspects.csv")

;; 这里想要做的事情就是把 .csv 文件中的内容转换成内存中的 seq of map

;; from file in hard disk to string in memory
(slurp filename)
;; => "Edward Cullen,10\nBella Swan,0\nCharlie Swan,0\nJacob Black,3\nCarlisle Cullen,6";; => "Edward Cullen,10\nBella Swan,0\nCharlie Swan,0\nJacob Black,3\nCarlisle Cullen,6"

(def mapkeys [:name :glitter-index])


;; split the long whole-file-as-string into seq of 2-items-vector
(defn convert
  [file-string]
  (map #(clojure.string/split % #",")
       (clojure.string/split file-string #"\n")))



(def person-infos
  (convert (slurp filename)))

;; convert string format glitter-index to int
(defn str->int
  [str]
  (Integer. str))


(defn get-person-info-maps
  ;; combine the [:name :glitter-index] format keys
  ;; with [["yuanl" "12"] ["yuanx" "32"] ["chq" "345"]] to build a seq of map
  ;; like [{:name "yuanl" :glitter-index 12} {:name "yuanx" 32}
  ;; {:name "chq" :glitter-index 345}]
  ;;
  ;; seq of vector, seq of vector => seq of map
  [keys pers]
  (reduce #(conj %1
               (assoc {}
                      (first keys) (first %2)
                      (second keys) (str->int (second %2))))
        []
        pers))


;; 上面的这种方式比较蠢: 这么多 first second 函数, 其实完全可以利用变量声明时的[ 解
;; 析匹配 ]机制来做到相同的事情. clojure 中很多集合类型, 对集合的处理是 clojure 的
;; 核心的核心, map reduce + [ 解析匹配 ] 机制可以有效的增强 clojure 的表达能力.
;;
;; 因为本身我们就是从 seq of vector => seq of map, 这种模式正是 (map ) 函数的g完
;; 美战场.

(defn better-get-person-info-maps
  [keys pers]
  (map (fn [[ v-name v-idx ]]
         (let [[k-name k-idx] keys]
           (assoc {} k-name v-name k-idx (str->int v-idx))))
       pers))


;; 下面的函数是 [clojure for the brave and truth] 书中实现的方法, 虽然复杂一些但是
;; 里面有几个很好用的编程技巧.
;; (1) 内部函数接受外部函数的参数, 比如 (map ... (reduce ...)) 可以直接作为已知量处理
;; (2) 内部函数尽量在一开始换行, 并通过注释举例说明接收和返回的类型:
;;     eg:
;;     (map (fn ;; ["yl" "13"] => {:name "yl" :glitter-index 13}
;;            ...
;;            (reduce (fn ;; ["yl" "13"] => ([:name "yl"][:glitter-index 13])
;;                      ))))
;; (3) 函数中涉及到要把 string 类型的 value 转换成 int 型, 但是你并不知道应该对
;; 哪个 value 进行转换, 而且其中一个 value 无法转成 int 型, 这时候可以考虑构造一
;; 个 map 结构, key 值为原来的 key, value 是一个函数, 如此一来通过 key 把对应的
;; value 和 函数关联起来.

;; 几个重点函数举例:

;; (assoc target-map k (func v)) ;; this function will apply on both name "yuanl"
;;                               ;; and glitter-index "13", of course that it will
;;                               ;; bring an error. So, what we want to do is
;;                               ;; the "func" can auto change according to the
;;                               ;; value it apply on, notice that we also have
;;                               ;; another parameter "k", that's give us a beam of
;;                               ;; light to get this work!

;; (def key-func-map
;;   {:name identity            ;; if it is a ":name" value, apply the "identity"
;;                              ;; function
;;    :glitter-index str->int}) ;; if it is a "glitter-index" value, apply
;;                              ;; the "str->int" function

;; (defn str->int
;;   [string]
;;   (Integer. string))

;; (assoc target-map k (Integer. v))              ; this will trigure an error, when v = "yuanl"

;; (assoc target-map k ((get key-func-map k) v))  ; When change into this, it will be safe

;; 总结起来, 书中介绍的这个过程非常有意义, 其核心是 [通过元素的放大再缩小实现转换]

;;                             /                                                                            \
;;                      缩小   |          放大成集合                              通过reduce                    |
;;              集合 --------->| 单元素 --------------> 单元素与其他元素组成的集合 ---------------> 融合成新的单元素   |
;;           [[yl 3]          |[yl 3]                 ([:name yl][:idx 3])                {:name yl :idx 3}  |
;;            [yx 4]          |    .                                                              ^          |
;;            [ch 5]]          \   .                                                              .          /
;;                                 ................................................................
;;                                       通过 map 把元素变成集合, 再通过 reduce 把集合融合成元素
;;                                       以此实现从元素到元素的 transformation.



(def key-func-map
  {:name identity
   :glitter-index str->int})

(defn book-get-person-info-maps
  [keys pers]
  (map (fn  ;; ["yl" "3"] -> {:name "yl" :glitter-index 13}
         [row]
         (reduce (fn ;; ([:name "yl"] [:glitter-index 13]) -> {:name "yl" :glitter-index 13}
                   [row-map [k v]]
                   (assoc row-map k ((get key-func-map k) v)))
                 {}
                 (map vector keys row) ;; ([:name "yl"] [:glitter-index 13])
                 )
         )
       pers))




(def person-info-maps (get-person-info-maps mapkeys person-infos))
(def person-info-maps (better-get-person-info-maps mapkeys person-infos))
(def person-info-maps (book-get-person-info-maps mapkeys person-infos))

(get-person-info-maps mapkeys person-infos) 
;; => [{:name "Edward Cullen", :glitter-index 10}
;;     {:name "Bella Swan", :glitter-index 0}
;;     {:name "Charlie Swan", :glitter-index 0}
;;     {:name "Jacob Black", :glitter-index 3}
;;     {:name "Carlisle Cullen", :glitter-index 6}]

(better-get-person-info-maps mapkeys person-infos) 
;; => ({:name "Edward Cullen", :glitter-index 10}
;;     {:name "Bella Swan", :glitter-index 0}
;;     {:name "Charlie Swan", :glitter-index 0}
;;     {:name "Jacob Black", :glitter-index 3}
;;     {:name "Carlisle Cullen", :glitter-index 6})

(book-get-person-info-maps mapkeys person-infos) 
;; => ({:name "Edward Cullen", :glitter-index 10}
;;     {:name "Bella Swan", :glitter-index 0}
;;     {:name "Charlie Swan", :glitter-index 0}
;;     {:name "Jacob Black", :glitter-index 3}
;;     {:name "Carlisle Cullen", :glitter-index 6})

(defn filter-glitter
  "find the records whose glitter-index >= 3
  [{} {} ...] -> [{} {} ...]"
  [info-maps]
  (filter #(>= (:glitter-index %) 3) info-maps))

(def glitter-filter-records (filter-glitter person-info-maps))
;; => ({:name "Edward Cullen", :glitter-index 10}
;;     {:name "Jacob Black", :glitter-index 3}
;;     {:name "Carlisle Cullen", :glitter-index 6})

glitter-filter-records

;; Exercises

;; The vampire analysis program you now have is already decades ahead of anything
;; else on the market. But how could you make it better? I suggest trying the
;; following:

;; 1. Turn the result of your glitter filter into a list of names.

(map :name glitter-filter-records) 
;; => ("Edward Cullen" "Jacob Black" "Carlisle Cullen")



;; 2. Write a function, append, which will append a new suspect to your list of
;; suspects.

(defn append
  [name idx file]
  (spit file (str name "," idx "\n") :append true))

;; (spit "yl.file" "did last line still there !!!yl\n" :append true)如果该文件不
;; 存在则会创建, 并存储内容进去

(append "yl" "10" filename)

(convert (slurp filename)) 
;; => (["Edward Cullen" "10"]
;;     ["Bella Swan" "0"]
;;     ["Charlie Swan" "0"]
;;     ["Jacob Black" "3"]
;;     ["Carlisle Cullen" "6"]
;;     ["yl" "10"])



;; 3. Write a function, validate, which will check that :name and :glitter-index
;; are present when you append. The validate function should accept two arguments:
;; a map of keywords to validating functions, similar to conversions, and the
;; record to be validated.



(defn validate
  [key-func-map [name idx]]
  (and ((get key-func-map :name) name)
       ((get key-func-map :glitter-idx) idx)))





;; 4. Write a function that will take your list of maps and convert it back to a
;; CSV string. You’ll need to use the clojure.string/join function.

;; Good luck, McFishwich!

;; person-info-maps

(defn spit-maps-into-file
  [pson-info-maps]
  (reduce (fn ;;
            [pson-info-map-to pson-info-map]
            (clojure.string/join "\n"
                                 [pson-info-map-to pson-info-map]
                                 ))
          (map map-to-val-vect person-info-maps)))

(defn map-to-val-vect
  ;; {:name yl :idx 13}
  ;; =>
  ;; "yl,13"
  [pson-info-map]
  (reduce (fn [item1 item2] ;; item1=yl, item2=13
            (clojure.string/join "," [item1 item2])) ;; "yl, 13"
          (let [[keys vals]
                (let [[name-vect idx-vect] (seq pson-info-map) ;; name-vect=[:name yl], idx-vect=[:idx 13]
                      ]
                  (map vector name-vect idx-vect) ;; ([:name :idx] [yl 13])
                  )]
            vals ;; [yl 13]
            )))



(spit-maps-into-file person-info-maps) 
;; => "Edward Cullen,10\nBella Swan,0\nCharlie Swan,0\nJacob Black,3\nCarlisle Cullen,6\nyl,10\nyl,10"


;; another method:
;; 这里并非另一种解法, 而是更深刻的理解了  map 以及 reduce:
;; 这两个函数都是表达能力[非常全面]的函数, 这次的理解主要在:
;; (1) 如果你需要对集合的子元素当做集合进行处理, 则应该放在 (map fn arg1 ...) 的 fn 中
;; (2) 如果你需要对集合整体的所有元素进行处理, 则应该放在 (map fn arg1 ...) 的 arg 中
;; (3) 拆分集合元素既可以通过 let 去做, 也可以通过 map or reduce 去做. 也就是直到你觉得可以整体
;;     一次性处理之前, 都应该把转换放在 fn 中. 因为每个 map 的 fn 都会更深入一层.
;; (4) 不论使用 reduce 还是 map 都需要考虑一个核心问题 "元素是看成个体处理还是看成集合处理", 如果需要则需要引入内层 map.
;;
;; 比如:
;; 本题第一步是将 [{:name yl :idx 13}          =>[[[:name yl] [:idx 13]]
;;              {:name yx :idx 14} ...] 变成 => [[:name yx] [:idx 14]]]
;; 这是一个整体变换且是并行的, 应该放在一个 map 中, 而且这一步变换应该放在 map 的集合参数中. 如 (a) 所示
;;
;; 本题第二步是将 [:name yl] [:idx 13] 变成 [yl 13] 这里就需要把每个 [[][]] 看成一个集合去处理, 这时候
;; 就需要把这一步变换放在 map 的 fn 中, 如 (b) 所示

(defn spit-maps-into-file2
  [pson-info-maps filename]
  (spit filename
        (reduce (fn ;;
                  [str-front str-back]
                  (clojure.string/join "\n" [str-front str-back]))
                (map (fn ;; (b)
                       [pson-info-vector]
                       (clojure.string/join "," (map second pson-info-vector)))
                     (map seq pson-info-maps) ;; (a)
                     ))))

(spit-maps-into-file2 person-info-maps filename)
