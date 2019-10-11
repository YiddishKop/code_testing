(ns the-divine-cheese-code.core
  ;;
  ;; 导入 clojure.java.browse 库给予别名 browse (该库下的所有函数通过 browse/ 前
  ;; 缀引用)
  ;;
  ;; 导入同工程下 src 目录下 the_divine_cheese_code 目录下 visualization 目录下
  ;; 的源代码 svg.clj 并将其 xml 注入当前 namespace 的 var-map 里（函数名直接使
  ;; 用）。
  (:require [clojure.java.browse :as browse]
            [the-divine-cheese-code.visualization.svg :refer [xml]])
  (:gen-class))


;; 这个源码文件是用来测试 require 的

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 三 : REAL PROJECTS ORGANIZATION    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (ns proj_name.file_name) 的功能类似 (in-ns ns-symbol) 都是创建并切换到该
;; namespace



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 3.1 文件名与命名空间名的对应关系 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; 在 clojure 中, namespace 和 path of file 存在一对一的映射关系：

;; 1. 当你通过 lein 创建了一个文件夹（工程），源代码的根目录默认就是 /src 目录
;; 2. namespace 名字中的破折号 “-”, 在文件名中体现为 “_”
;; 3. namespace 名字中的 "." [ 前面 ]的部分在文件系统中是[ 文件夹名称 ]
;; 4. namespace 名字中的 "." [ 后面 ]的部分在文件系统中是[ 子文件夹 ]名称 or [ 源代码文件 ]名称


;; 假如一个 namespace 是这样的： 
;;
;; the-divine-cheese-code.visualization.svg
;;
;; 那么他对应的文件系统目录为：
;;
;; src --+
;;       |-- the_divine_cheese_code --+
;;                                    |-- visualization +
;;                                                      |-- svg.clj


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 3.2 require 的工作原理及其与文件系统的关系 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; 当源代码中出现 (require the-divine-cheese-code.visualization.svg) 的时候， 他
;; 就会按照上面的 4 个原则寻找 svg.clj，然后
;; (1) 编译之
;; (2) 创建其 namespace
;; (3) 将其导入 require 语句出现的源文件中
;; (4) 可以全路径访问其中的 var


;; 下面这个语句， clojure 会读取 the-divine-cheese 工程 src 目录下的 visualization
;; 目录下的 svg.clj 文件，并编译它。要想编译该文件就必须先创建
;; the-divine-cheese-code.visualization.svg 名称空间， 并编译其中的两个函数


;; 需要注意的是：尽管已经存在了这个源码文件，但是 clojure 在运行该工程的时候并不
;; 会自动编译它，所以需要手动指定才可以


;; require 只负责引入 clojure 库，不绑定函数到当前的 ns 中

;; 不引入库， 源码文件就只是一堆文件，不会被 clojure 编译，也就没法在其他名字空
;; 间中使用， 如果需要在其他名字空间中使用，第一件事情就是编译成类，也就是需要使
;; 用如下 require 语句，他的意思相当于：[ 请帮我编译指定名字空间对应的源代码文件
;; 并将其引入到当前名字空间中，这样我就可以通过全名对其进行引用 ].


;; 在 REPL 中别的名字空间的 var 不需要 require 可以引用， 但是在 Project 中由于名字
;; 空间对应的是具体的源代码文件（一个名字空间对应一个源代码文件），而 require 自带
;; 编译效果，所以没有 require 的话是没法直接使用的。


;; [总结]
;; require = 编译 + 使用全名
;; refer   = 使用简名


;; (require 'the-divine-cheese-code.visualization.svg)

;; 有了这一句之后， 在本空间（文件）内就可以直接使用 points 而不是全路径的 points 了。
;; (refer 'the-divine-cheese-code.visualization.svg)

;; 没有执行上面那一句的话，下面这一句会报错： ClassNotFoundException
;; 有了上面的 require 代码，下面的才能正常执行.
(the-divine-cheese-code.visualization.svg/points [{:lat 223 :lng 23}])

(def heists [{:location "cologne, germany"
              :cheese-name "archbishop hildebold's cheese pretzel"
              :lat 50.95
              :lng 6.97}
             {:location "zurich, switzerland"
              :cheese-name "the standard emmental"
              :lat 47.37
              :lng 8.55}
             {:location "marseille, france"
              :cheese-name "le fromage de cosquer"
              :lat 43.30
              :lng 5.37}
             {:location "zurich, switerland"
              :cheese-name "the lesser emmental"
              :lat 47.37
              :lng 8.55}
             {:location "vatican city"
              :cheese-name "the cheese of turin"
              :lat 41.90
              :lng 12.45}
             ])



(def my-heists [
                {:location "china, nanjing"
                 :cheese-name "bread talk"
                 :lat 98.11
                 :lng 112.12}
                {:location "china, xuzhou"
                 :cheese-name "85c"
                 :lat 90.88
                 :lng 99.12}
                {:location "china, huaian"
                 :cheese-name "red car"
                 :lat 92.88
                 :lng 99.88}
                ])





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; require + :as = require + alias         ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(require '[the-divine-cheese-code.visualization.svg :as svg])
;; 上面这句， 等于下面两句：
;; (1)
(require 'the-divine-cheese-code.visualization.svg)
;; (2)
(alias 'svg 'the-divine-cheese-code.visualization.svg)
;; 如此可以使用简写的 namespace 进行函数调用
(svg/points heists)




;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; use = require + refer ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;

(use 'the-divine-cheese-code.visualization.svg)
;; 上面这句， 等于下面两句：
;; (1)
(require 'the-divine-cheese-code.visualization.svg)
;; (2)
(refer 'the-divine-cheese-code.visualization.svg)
;; 如此可以直接使用函数名对函数调用



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; use + :as = require alias ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 虽然这样做有点多此一举，因为 use = require + refer ,因为 refer 已经把目标
;; namespace 中所有的 var 注入当前 namesapce 的 ns-map 中. 但这种用法依然非常
;; 流行

(use '[the-divine-cheese-code.visualization.svg :as svg])
(= svg/points points)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; use + :only/:exclude/:rename = require + refer :only/:exclude/:rename ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 之前说过 refer 可以接一些 filter 用来限制加入进当前 namespace 的 var，具体的
;; filter 包括：
;; (refer 'the-divine-cheese-code.visualization.svg :filter ['var_A 'var_B 'var_C])
;; (1) :only    eg. :only    ['var_A 'var_B 'var_C]
;; (2) :exclude eg. :exclude ['var_A 'var_B 'var_C]
;; (3) :rename  eg. :rename  {'var_A 'A}

;; use 可以使用相同的语法结构

(use '[the-divine-cheese-code.visualization.svg :as svg :only [points]])




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; require, alias, refer, use 语法总结 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (require 'namespace)
;; (require '[ namespace :as xxx ]) ;; require 使用 as 的时候必须加 []

;; (refer 'namespace)
;; (refer 'namespace :only/:exclude ['A 'B 'C] / :rename {'A 'a})

;; (alias 'ns-alias 'namespace)

;; (use 'namespace)
;; (use '[ namespace :as xxx  :only/:exclude [A B C] / :rename {A a} ])





;;;;;;;;;;;;;;;;;;
;; The ns macro ;;
;;;;;;;;;;;;;;;;;;

;; 使用 ns 可以带来很多好处

;; 1. ns 会自动 refer clojure.core 名字空间
      ;; 所以你不需要使用 println 的全名：
      ;; (clojure.core/println "xxx").
      ;; 而是直接使用简名：
      ;; (println "xxxx")

;; 2. 你还可以通过 :refer-clojure + filter 来控制要 refer clojure.core 中的哪些 var
      ;;
      ;; [注意] TODO
      ;; 任何一个新建的 .clj 文件都会默认 require clojure.core, 
      ;; (ns the-divine-cheese-code.core
      ;;   (:refer-clojure :exclude [println]))
      ;;
      ;; (:refer-clojure xxxx) ':refer' 在 clojure 中有个专有的称呼叫
      ;; 做 [reference]， 他是 clojure 中的特殊语法。
      ;;
      ;; 等同于如下两条语句：
      ;; (in-ns 'the-divine-cheese-code.core)
      ;; (refer 'clojure.core :exclude ['println])


;; 六种 reference :
;; 1. (:refer-clojure)
;; 2. (:require)
;; 3. (:use)
;; 4. (:import)
;; 5. (:load)
;; 6. (:gen-class)






;;;;;;;;;;;;;;
;; :require ;;
;;;;;;;;;;;;;;

;; 语法格式为：

;; (:require namespace :as ns-shortname :refer [vars]/:all)


;; 与 require 用法类似（除了 quote 标记）：


;; (ns the-divine-cheese-code.core
;;   (:require [the-divine-cheese-code.visualization.svg :as svg]
;;             [clojure.java.browse :as browse]))

;; 等同于

;; (in-ns 'the-divine-cheese-code.core)
;; (require ['the-divine-cheese-code.visualization.svg :as 'svg])
;; (require ['clojure.java.browse :as 'browse])


;; 不同之处在于 ns 的 require 可以直接进行 refer 操作, 而 require 语句则不可以接
;; refer. ns 的 refer 可以接 :only [var-name] 或者 :all.

;; (ns the-divine-cheese-code.core
;;   (:require [the-divine-cheese-code.visualization.svg :refer [points]]))

;; 等同于

;; (in-ns the-divine-cheese-code.core)
;; (require 'the-divine-cheese-code.visualization.svg)
;; (refer 'the-divine-cheese-code.visualization.svg :only ['points])

;; 注： require 和 refer 前者是可以用，后者是直接用





;;;;;;;;;;
;; :use ;;
;;;;;;;;;;

;; (ns the-divine-cheese-code.core
;;   (:use clojure.java.browse))

;; 等同于

;; (in-ns 'the-divine-cheese-code.core)
;; (use 'clojure.java.browse)



;; 当你把 :use 的参数用 ‘[]’ 框起来时，表示用 '[]' 第一个元素作为前缀，'[]'第
;; 二个及后面所有的元素都以:

;; [first-element].[sencond-element]
;; [first-element].[third-element]
;; [first-element].[fourth-element]
;; .....

;; 的形式进行组合，而后通过 use 包含该库并合并到当前 namespace 的 intern map 里。

;; (ns the-divine-cheese-code.core
;;   (:use [clojure.java browse io]))

;; 等同于

;; (in-ns 'the-divine-cheese-code.core)
;; (use 'clojure.java.browse)
;; (use 'clojure.java.io)




(defn url
  "
  把当前文件夹地址与给定的文件名进行拼接

  "
  [filename]
  (str "file:///"
       (System/getProperty "user.dir") ;;DONE

       ;; [编程感想：System/getProperty]
       ;; 这个是获取系统相关路径的函数， 最常用的参数有如下三个：
       ;; (1) "user.name"   => 获取系统用户名字符串
       ;; (2) "user.home"   => 获取系统家目录字符串
       ;; (3) "user.dir"    => 获取当前工程根目录字符串
       ;;
       ;; 实例如下：
       ;; (System/getProperty "user.name")
       ;; "yiddi"
       ;; (System/getProperty "user.home")
       ;; "/home/yiddi"
       ;; (System/getProperty "user.dir")


       "/"
       filename))
(url "anaconda3") 
;; => "file:////home/yiddi/git_repos/code_testing/clojure/the-divine-cheese-code/anaconda3"



(defn my-url
  [filename]
  (str (System/getProperty "user.dir")
       "/"
       filename))



(defn template
  [contents]
  (str "<style>polyline {fill:none;stroke:#5881d8;stroke-width:3}</style>"
       contents))
(template "hello yl")






(defn -main
  [& args]
  (let [filename "map.html"]
    (->> heists
         (xml 50 100)
         template
         (spit filename)) ;;DONE

    ;; [编程感想： spit and slurp]
    ;; (spit filename content-str :append true)
    ;; spit 是“吐口水”的意思，往文件中“吐”东西, "spit someplace something"
    ;; (slurp filename)
    ;; slurp 是“吃的声音”的意思, 从文件中“吃”东西，"slurp all thing from someplace"

    (browse/browse-url (url filename))) ;;DONE

  ;; [编程感想： 如何在 clojure 中命令浏览器打开指定网址]
  ;; (clojure.java.browse/browse-url url-str) 使用系统默认浏览器打开 url
  )

(defn -my-main
  "

  这里需要完成的功能是： 把 heists 路线交给 xml 函数生成 svg 的 polyline 内容，
  写入内容通过 template 拼接到 <style> 之后，然后通过 spit 将内容写入指定的
  map.html 文件中，然后通过 url 获取该 html 文件的全路径， 最后将全路径交给
  browser/browse-url 展示 html 文件.

  "
  [& args]
  (let [filename "map.html"]
    (->> heists
         (xml 50 100)
         template
         (spit filename))
    (browse/browse-url (url filename))))
