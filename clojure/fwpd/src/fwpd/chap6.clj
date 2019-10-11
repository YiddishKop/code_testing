(ns fwpd.chap6)
;;  ----------
;;    ^
;;    |
;; namespace




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ONE: Storing Objects with def ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; namespace                      ==> 书架
;; vars(or called 'address')      ==> 书的位置=(书架/书名)

;; Namespace contains map between human-friendly symbols and references to shelf
;; addresses, known as vars, much like a card catalog

;; 【注意】var即地址即内容, get var means get address means get the object.






;;;;;;;;;;;;;
;; ns-name ;;
;;;;;;;;;;;;;



;; 读取当前运行环境的名字空间
(ns-name *ns*) 
;; => fwpd.chap6

;; 等同于 

(ns-name 'fwpd.chap6)
;; => fwpd.chap6


;; 很重要的三个关键词： 记号(symbol) -> 地址(var/address)
;;                                    |
;;                                   物品(object)


;; 所有出现在 clojure 的字符（包括关键字和函数）都是 symbol. symbol 在clojure中
;; 是一种类型，单独出现的 symbol 都会被编译器理解为一个地址（eval 过程）, 如果仅
;; 仅想引用而不计算，需要 quote

;; 科技类书架 ===> 科技类书架的地址
;; '科技类书架 ===> 科技类书架名称


inc
;; => #function[clojure.core/inc]

'inc
;; => inc


(map inc [1 2]) 
;; => (2 3)

'(map inc [1 2]) 
;; => (map inc [1 2])






;;;;;;;;;;;;;;;;;;;;;;;;;
;; ns-interns / ns-map ;;
;;;;;;;;;;;;;;;;;;;;;;;;;



;; 获取名字空间中 *自己建立的* 已经存储的 [ symbol var ] 对
;; 注意，他是一个实实在在的 map 数据结构
;; ns-interns 函数用来获取当下名字空间中的 [记号-地址] 对
;; *ns* 特别有 emacs 的命名特点，表示当下的名字空间
;; *ns* = 'fwpd.chap6

(ns-interns *ns*) 
;; => {}

(ns-interns 'fwpd.chap6) 
;; => {}

(def great-books ["East of Eden" "The algorithms"])

(ns-interns *ns*) 
;; => {great-books #'fwpd.chap6/great-books}
;;     ----------- ------------------------
;;     symbol      reader form of var

;; 既然是 map数据结构，就可以通过get/get-in 来获取key的 value

(get (ns-interns *ns*) 'great-books) 
;; => #'fwpd.chap6/great-books


;; 如果想获取名字空间中 *所有的* [symbole var] 对， 包括自己建立的/默认继承的就
;; 需要使用另一个方法： ns-map. 这个方法会打印非常非常多的东西， 所以一般都结合
;; get 使用，查询制定的 key 的 value.

(ns-map *ns*)



;;;;;;;;;;;
;; deref ;;
;;;;;;;;;;;



;; 如何获取其他名字空间的对象

;; #' 可以理解为一个“抓取”函数，把指定记号(symbol)的内容(address)抓取过来
;; (1) 获取一个 var 的 reader form. eg #'fwpd.chap6/great-books
;; (2) 使用 deref

(def great-books ["East of Eden" "The algorithms"])

(deref #'fwpd.chap6/great-books)
;; ["East of Eden" "The algorithms"]

(def great-books ["beauty girl" "interior design"])

(deref #'fwpd.chap6/great-books)
;; ["beauty girl" "interior design"]




;;;;;;;;;;;;;;;;;;;;;;
;;; 两种方式获取对象 ;;;
;;;;;;;;;;;;;;;;;;;;;;


(def great-books ["East of Eden" "The algorithms"])

;; => 经过 def/defn 定义的内容等同于把如下三个部分进行连接

;; 记号 <=> 变量 <=> 对象
;;
;; great-books #'fwpd.chap6/great-books ["East of Eden" "The algorithms"]
;; ----------- ------------------------ ---------------------------------
;; symbol      reader form of var       object

;; 其中 [记号,变量] 之间的关系可以通过 (ns-interns *ns*) 来查询获得
(ns-interns *ns*)
{great-books #'fwpd.chap6/great-books}

;; 所以有两种方式来获取对象：
;;
;; 一，通过 symbol 直接获取
great-books

;; 二，通过 deref var 间接获取
(deref #'fwpd.chap6/great-books)




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TWO: Creating and Switching to Namespaces ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; 如何创建 namespace
;; 1. (create-ns symbol)
;; 2. (in-ns symbol)
;; 3. (ns)


(create-ns 'cheese.taxonomy)
(ns-name (create-ns 'cheese.taxonomy))


;; 如何使用其他 namespace 的变量和函数: namespace/name:
;; '/' 与普通 OO 语言中的 '.' 类似。

fwpd.chap6/great-books



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; refer :only :exclude :rename ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; =================================================
;;
;; refer 类似 import, 但是略有不同，在 namespace_A 中调用 (clojure.core/refer
;; namespace_B) 等同于：
;;
;;
;; 1. 调用 (ns-interns namespace_B) 获取 namespace_B 中自己建的 [ symbole-var ] map
;; 2. 将上面的 [ symbole-var ] map 保持不变合并到 (ns-map namespace_A) 中
;;
;; 也就是说，在 namespace_B中定义的：
;; (def hello-str "hello")

;; 其对应的 [symbol-var] map 为：
;; (ns-interns 'namespace_B)
;; => {hello-str #'namespace_B/hello-str}

;; 在 namespace_A 中定义的：
;; (def goodbye-str "goodbye")
;; 其对应的 [symbol-var] map 为：
;; (ns-interns 'namespace_A)
;; => {goodbye-str #'namespace_A/goodbye-str}

;; 然后在 namespace_A 中执行
;; (refer 'namespace_B)
;; namespace_A 的 intern map 就会发生改变：
;; 从：
;; {goodbye-str #'namespace_A/goodbye-str}
;; 变成：
;; {hello-str   #'namespace_B/hello-str
;;  goodbye-str #'namespace_A/goodbye-str}


;;
;; :only    表示只合并制定的部分
;; :exclude 表示合并除了指定部分的所有其他部分
;; :rename  表示对 symbol-var 中的 symbol 重新命名再合并进来
;;
;;
;; 使用 clojure.core/refer 之前，如果想仅仅通过函数名称引用 fwpd.chap5/test-dri
;; 的话，会直接报错：

test-dri 
;; => error

;; 通过 refer 引用之后就不会存在这种问题
(clojure.core/refer 'fwpd.chap5)

test-dri 
;; => #function[fwpd.chap5/test-dri]
;;
;; =================================================





;; =================================================
;;
;; 通过 clojure.core/refer 引入当前 namespace的并非自己建立的，所以在 ns-interns
;; 中查不到， 只能到 ns-map 中通过 get 来查询

(ns-interns *ns*)             ;; 这里没有
(get (ns-map *ns*) 'test-dri) ;; 这里有
;; =================================================






;; =================================================
;;
;; 可以在 clojure.core/refer 之后添加一些 filter 用来对合并进来的 symbol-var 对
;; 进行过滤，可以填写的 filter 包括： ：only :exclude :rename
(clojure.core/refer 'fwpd.chap5 :only ['count-char])

(count-char "ssss")


(clojure.core/refer 'fwpd.chap5 :exclude ['count-char])

(count-char "sdfsdfsdf")


(clojure.core/refer 'fwpd.chap5 :rename {'count-char 'ctch})

(ctch "sdfsdfsdf")
;; =================================================






;; =================================================
;; 如何快速的从 clojure.core 名字空间中继承所有 symbol-var 对

(clojure.core/refer-clojure)
;; =================================================





;; =================================================
;;
;; 如何建立仅仅针对某些函数可见的函数（private functions）
;;
;;
;; 可以使用 defn- 来建立私有函数, 在 namespace_A 中定义如下函数
;; (defn- private-function "Just an example" [])

;; 在 namespace_B 中想通过全路径名访问或者想refer到当前namespace都是无效的，会提
;; 示: private-function is not public

;; ERROR: (clojure.core/refer 'namespace_A :only ['private-function])
;; ERROR: (namespace_A/private-function)
;; =================================================





;;;;;;;;;;;
;; alias ;;
;;;;;;;;;;;


;; =================================================
;; alias 的意思就简单很多， 他是给namespace 取一个简单的别名，使其方便使用
;;
;; (clojure.core/alias 别名 名字空间)
;;
(clojure.core/alias 'chap5 'fwpd.chap5)

chap5/not-private
;; =================================================





