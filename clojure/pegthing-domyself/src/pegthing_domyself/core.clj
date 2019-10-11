(ns pegthing-domyself.core
  (:gen-class))

(defn test-dri
  [expt actual]
  (condp = (= expt actual)
    true "PASS"
    false (str "EXPECT is: " expt "\n ACTUAL is: " actual)))

(def test-board
  "valid move:
  1 -> 2 -> 4
  6 -> 5 -> 4"
  {:rows 3
   1 {:pegged true :connect {4 2 6 3}}
   2 {:pegged true :connect {}}
   3 {:pegged true :connect {}}
   4 {:pegged false :connect {1 2 6 5}}
   5 {:pegged true :connect {}}
   6 {:pegged true :connect {1 3 4 5}}})


;;;;;;
;;; 基础数据结构
;;;;;;


;;;;;;;;;;;;;;;;;
;; 一，辅助 API ;;
;;;;;;;;;;;;;;;;;


;; 获取行尾结尾无限集合
(defn get-rowends-seq
  ([] (get-rowends-seq 1 1))
  ([current row-num]
   (lazy-seq (cons current (get-rowends-seq (+ 1 current row-num) (inc row-num))))))
(test-dri [1 3 6 10] (take 4 (get-rowends-seq)))
(def ends-seq (get-rowends-seq))


;; 输入一个位置获取其行数
(defn get-row-num
  [pos] ((comp inc count) (take-while #(> pos %) ends-seq)))
(test-dri 1 (get-row-num 1))
(test-dri 2 (get-row-num 3))
(test-dri 3 (get-row-num 5))


;; 输入行数获取行尾元素
(defn get-row-end
  [row] (last (take row ends-seq)))
(test-dri 1 (get-row-end 1))
(test-dri 3 (get-row-end 2))
(test-dri 10 (get-row-end 4))


;; 输入元素确定是否行尾
(defn row-tail?
  [pos]
  (= pos (get-row-end (get-row-num pos))))
(test-dri true (row-tail? 1))
(test-dri false (row-tail? 2))
(test-dri true (row-tail? 3))
(test-dri false (row-tail? 5))
(test-dri true (row-tail? 6))


;; 确定是否越界
(defn in-boundary?
  [max-pos & positions]
  (= max-pos (apply max max-pos positions)))
(test-dri true (in-boundary? 5 1 2 3))
(test-dri false (in-boundary? 1 1 2 3))









;;;;;;;;;;;;;;;;;
;; 二，转换 API ;;
;;;;;;;;;;;;;;;;;



;; 公共连接操作
(defn connect
  [board max-pos pos neighbor destination]
  (if (in-boundary? max-pos pos neighbor destination)
    (reduce (fn [cache-board [p1 p2]]
              (assoc-in cache-board [p1 :connect p2] neighbor))
            board
            [[pos destination] [destination pos]])
    board))
(test-dri {1 {:connect {4 2} } 4 {:connect {1 2} }} (connect {} 10 1 2 4))

;; 输入位置获取向右连接信息
(defn connect-right
  [board max-pos pos]
  (let [neighbor (inc pos)
        destination (inc neighbor)]
    (if (or (row-tail? pos) (row-tail? neighbor))
      board
      (connect board max-pos pos neighbor destination))))
(test-dri {} (connect-right {} 1 1))
(test-dri {4 {:connect {6 5} } 6 {:connect {4 5} }} (connect-right {} 10 4))


;; 输入位置获取向左下连接信息
(defn connect-down-left
  [board max-pos pos]
  (let [neighbor (+ pos (get-row-num pos))
        destination (+ neighbor (get-row-num neighbor))]
    (connect board max-pos pos neighbor destination)))
(test-dri {1 {:connect {4 2} } 4 {:connect {1 2} }} (connect-down-left {} 5 1))
(test-dri {} (connect-down-left {} 10 4))
(test-dri {4 {:connect {11 7} } 11 {:connect {4 7} }} (connect-down-left {} 15 4))


;; 输入位置获取向右下连接信息
(defn connect-down-right
  [board max-pos pos]
  (let [neighbor (+ pos 1 (get-row-num pos))
        destination (+ neighbor 1 (get-row-num neighbor))]
    (connect board max-pos pos neighbor destination)))
(test-dri {1 {:connect {6 3} } 6 {:connect {1 3} }} (connect-down-right {} 10 1))
(test-dri {4 {:connect {13 8} } 13 {:connect {4 8} }} (connect-down-right {} 15 4))
(test-dri {} (connect-down-right {} 10 4))


;; 创建并初始化棋盘的 pegged 和 rows 数据
(defn pegged-rows-init
  [rows]
  (let [max-pos (get-row-end rows)
        cache-board {:rows rows}]
    (reduce (fn [board pos]
              (assoc-in board [pos :pegged] true))
            cache-board
            (range 1 (inc max-pos)))))
(test-dri {:rows 1 1 {:pegged true}} (pegged-rows-init 1))
(test-dri {:rows 2 1 {:pegged true} 2 {:pegged true} 3 {:pegged true}} (pegged-rows-init 2))


;; 创建单个棋子的连接数据
(defn build-connection
  [board max-pos pos]
  (reduce (fn [cache-board connector]
            (connector cache-board max-pos pos))
          board
          [connect-right connect-down-left connect-down-right]))
(test-dri {1 {:connect {4 2 6 3} } 4 {:connect {1 2} } 6 {:connect {1 3} }} (build-connection {} 10 1))
(test-dri {1 {:connect {4 2} } 4 {:connect {1 2} }} (build-connection {} 5 1))


;; 初始化整个棋盘
(defn init-board
  [rows]
  (reduce (fn [board pos]
            (build-connection board (get-row-end rows) pos))
          (pegged-rows-init rows)
          (range 1 (inc (get-row-end rows)))))
(test-dri {:rows 3
           1 {:pegged true :connect {4 2 6 3}}
           2 {:pegged true}
           3 {:pegged true}
           4 {:pegged true :connect {1 2 6 5}}
           5 {:pegged true}
           6 {:pegged true :connect {1 3 4 5}}}
           (init-board 3))







;;;;;;;;;;;;;;;;;
;; 三，功能 API ;;
;;;;;;;;;;;;;;;;;

;; 获取某个位置的 peg
(defn peg-get
  [board pos]
  (get-in board [pos :pegged]))
(test-dri true (peg-get {1 {:pegged true}} 1))

;; 移除某个位置的 peg
(defn peg-remove
  [board pos]
  (assoc-in board [pos :pegged] false))
(test-dri {1 {:pegged false}} (peg-remove {1 {:pegged true}} 1))


;; 添加某个位置的 peg
(defn peg-place
  [board pos]
  (assoc-in board [pos :pegged] true))
(test-dri {1 {:pegged true}} (peg-place {1 {:pegged false}} 1))


;; peg跳跃
(defn peg-move
  [board pos destination]
  (let [neighbor (get-in board [pos :connect destination])
        neb-peg (get-in board [neighbor :pegged])
        des-peg (get-in board [destination :pegged])]
    (if (and neb-peg (not des-peg))
      (peg-remove
       (peg-remove
        (peg-place board destination)
        neighbor)
       pos)
      board)))
(test-dri {1 {:pegged true :connect {4 2}}
           2 {:pegged true}
           3 {:pegged true}
           4 {:pegged true :connect {1 2}}}
          (peg-move {1 {:pegged true :connect {4 2}}
                     2 {:pegged true}
                     3 {:pegged true}
                     4 {:pegged true :connect {1 2}}}
                    1 4))
(test-dri {1 {:pegged true :connect {4 2}}
           2 {:pegged false}
           3 {:pegged true}
           4 {:pegged true :connect {1 2}}}
          (peg-move {1 {:pegged true :connect {4 2}}
                     2 {:pegged true}
                     3 {:pegged true}
                     4 {:pegged false :connect {1 2}}}
                    1 4))


;; 展示某个位置所有可以跳跃的目的地
(defn peg-valid-moves
  [board pos]
  (filter #(valid-move? board pos %)
        (map first (get-in board [pos :connect]))))
(test-dri [4] (peg-valid-moves test-board 1))
(test-dri [4] (peg-valid-moves test-board 6))
(test-dri [] (peg-valid-moves test-board 4))


;; 判断某个跳转是否有效
(defn valid-move?
  [board pos destination]
  (if-let [neighbor (get-in board [pos :connect destination])]
    (and (peg-get board pos)
         (peg-get board neighbor)
         (not (peg-get board destination)))
    nil))
(test-dri true (valid-move? test-board 1 4))
(test-dri false (valid-move? test-board 4 1))
(test-dri false (valid-move? test-board 4 6))
(test-dri true (valid-move? test-board 6 4))



;; 棋盘终结状态判断
(defn can-move?
  [board]
  (some #((complement empty?) %)
        (map #(peg-valid-moves board (first %))
             (filter #(get-in (second %) [:pegged]) board))))
(test-dri true (can-move? test-board))
(test-dri nil (can-move? (assoc-in (assoc-in test-board [1 :pegged] false) [6 :pegged] false)))






;;;;;;;;;;;;;;;;;
;; 棋盘打印 API ;;
;;;;;;;;;;;;;;;;;


;; 定义字符常量
(def alpha-start (int \a)) ;;97
(def alpha-end (int \z)) ;;122
(def letters (map (comp str char) (range alpha-start (inc alpha-end))))
(def char-pos 3)



;; 定义颜色映射关系
(def ansi-style
  {:red   "[31m"
   :green  "[32m"
   :blue  "[34m"
   :reset "[0m"
   })


;; 给指定字符串上色
(defn str-colorize
  [string style]
  (str "\u001b" (style ansi-style) string "\u001b" (:reset ansi-style)))
(str-colorize "hello world" :red)


;; 打印行内容
(defn row-char-render
  [board row-num]
  (clojure.string/join (apply str (repeat char-pos " "))
                       (map (fn [pos]
                              (str (nth letters (dec pos))
                                   (if (get-in board [pos :pegged])
                                     (str-colorize "0" :green)
                                     (str-colorize "-" :red))))
                            (range (inc (- (get-row-end row-num) row-num))
                                   (inc (get-row-end row-num))))))
(test-dri (str "a" (str-colorize "0" :green)) (row-char-render test-board 1))
(row-char-render test-board 3)


;; 行内容占用字符数
(defn row-char-count
  [row-num]
    (+ (* 2 row-num) (* char-pos (dec row-num))))
(test-dri 2 (row-char-count 1))
(test-dri 7 (row-char-count 2))


;; 打印行前空格
(defn row-front-render
  [board row-num]
  (let [rows (get-in board [:rows])
        spc-num (/ (- (row-char-count rows)
                      (row-char-count row-num))
                   2)]
    (apply str (repeat spc-num " "))))
(test-dri "     " (row-front-render test-board 1))
(test-dri "" (row-front-render test-board 3))


;; 打印棋盘 [表象数据结构]
(defn print-board
  [board]
  (let [rows (get board :rows)]
    (doseq [row (range 1 (inc rows))]
      (println (str (row-front-render board row)
                    (row-char-render board row))))))
(print-board test-board)





;;;;;;;;;;;;;;;;;
;; 四，用户交互 ;;
;;;;;;;;;;;;;;;;;


;; 协助函数，字符串转成pos

(defn ui-hp-str2p
  [string]
  (inc (- (int (first (clojure.string/lower-case
                       (clojure.string/trim string))))
          alpha-start)))
(test-dri 3 (ui-hp-str2p "  C"))
(test-dri 1 (ui-hp-str2p "a  "))


;; 协助函数，字符串转成 pos destination
(defn ui-hp-str2pd
  [string]
  (map ui-hp-str2p
       (re-seq #"[a-zA-Z]" (clojure.string/trim string))))
(test-dri [1 3] (ui-hp-str2pd "A-c0"))


;; 输入行数创建 board
(defn ui-build-board
  []
  (println "Input the row number of board [5]:")
  (let [rows-str (clojure.string/trim (read-line))
        rows (Integer. (if (empty? rows-str) "5" rows-str))]
    (ui-remove-peg (init-board rows))))


;; 输入移除peg的位置
(defn ui-remove-peg
  [board]
  (println "The board is:")
  (print-board board)
  (println "Input the position you want to remove: ")
  (let [pos (ui-hp-str2p (read-line))]
    (ui-move-peg (peg-remove board pos))))


;; 输入起/终点移动peg
(defn ui-move-peg
  [board]
  (println "The board is:")
  (print-board board)
  (println "Input from and to letters:")
  (let [[pos destination] (ui-hp-str2pd (clojure.string/trim (read-line)))
        cache-board (peg-move board pos destination)]
    (if (can-move? cache-board)
      (ui-move-peg cache-board)
      (ui-end-game cache-board))))


;; board进入终结态收尾处理
(defn ui-end-game
  [board]
  (println "The board is:")
  (print-board board)
  (let [remains (count (filter #(:pegged (second %)) board))
        end-input (do (println "Go on [n]ew game or [e]nd? [n]")
                      (clojure.string/lower-case
                       (clojure.string/trim (read-line))))]
    (println "The remain # of peg is: " remains)
    (if (or (= end-input "n") (= end-input ""))
      (ui-build-board)
      (System/exit 0))
    ))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ui-build-board)
  )
