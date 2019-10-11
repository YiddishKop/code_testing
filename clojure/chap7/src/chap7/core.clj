(ns chap7.core
  (:gen-class))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CHAP 7                                           ;;
;; Clojure Alchemy: Reading, Evaluation, and Macros ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Macros allow you to transform arbitrary expressions into valid Clojure, so you
;; can extend the language itself to fit your needs


(defmacro backwards
  [form]
  (reverse form))

(backwards ("backwards" "am" "I" str))
;; => "Iambackwards"

(reverse '("backwards" "am" "I" str))
(eval (reverse '("backwards" "am" "I" str)))


;; In macro expression
;; args ----> as symbols ----> passed to macro body ----> eval the result

;; In functionn expression
;; args ----> as values ----> passed to function body ----> eval the result


(defn backw
  [form]
  (reverse form))

(backw ("backwards" "am" "I" str))
;; => error

(backw ["a" "b" "c"])
;; => ("c" "b" "a")



;; This chapter explains the elements of Clojure’s evaluation model: the
;; reader, the evaluator, and the macro expander. It’s like the periodic table
;; of Clojure elements


;; Clojure (like all Lisps) has an evaluation model that differs from most other
;; languages:

;; (1) it has a *two-phase* system where it reads textual source code, producing
;; Clojure data structures

;; (2) These data structures are then evaluated: Clojure traverses the data
;; structures and performs actions like function application or var lookup based
;; on the type of the data structure: like, Clojure reads the text (+ 1 2), the
;; result is a list data structure whose first element is a + symbol, followed
;; by the numbers 1 and 2. This data structure is passed to Clojure’s
;; evaluator, which looks up the function corresponding to + and applies that
;; function to 1 and 2

;; 读取[ 代码文本 ]，产生[ 数据结构 ]，依据数据结构类型找到对应的[ 计算规则 ]。

;; Languages that have this relationship between source code, data, and
;; evaluation are called homoiconic.

;;             Parser & Lexer
;; Code -----------+                                                  Machine Code 10110001101
;; (text)          |                                                         ^
;;                 +---------------> AST --------------> Evaluator ----------+
;;                                (tree in compiler
;;                                 transparent for language)

;; Evaluator a function that traverses the tree to produce the machine code or
;; whatever as its output.

;; However, in most languages the *AST*’s data structure is *INACCESSIBLE*
;; within the programming language, the programming language space and the
;; compiler space are forever separated

;; 大部分语言的编译器（or解释器）产生的 AST 是不能被编程语言直接访问的，也就是编
;; 译器产生的中间结果（AST --- abstract syntax tree）对于程序语言是完全 *不可见*
;; 的。

;; https://www.braveclojure.com/assets/images/cftbat/read-and-eval/non-lisp-eval.png


;; 但是 clojure （和其他lisp 方言）却完全不同， clojure 并不以 AST 作为
;; Evaluator 的输入， 而是使用自身的数据结构（NDS --- native data structure）作
;; 为 Evaluator 的输入. 自身的数据结构是什么？ 就是 clojure 的list以及数值。list
;; 可以很方便的直译为树结构---‘树’是逻辑结构，list是物理结构。



;; clojure 的 Reader 读入list 结构，将其转换成嵌套的树结构，然后 Evaluator 读入
;; 这个树结构并编译为 JVM 代码。


;;               Reader
;; Code  ----------+                                                       Value 45
;; (text)          |                                                         ^
;;                 +---------------> NDS --------------> Evaluator ----------+
;;                               (list in language)          ^
;;                                                           |
;;                                                           |
;;                             (list defined by user) -------+ eval



(def addition-list '(+ 1 2))
(eval addition-list)

;; 注意，symbol 和 list 在 clojure 语言中都比较特殊，因为他们有完全相同的表现形
;; 式： '. 但是quote 之于两者产生了不同的

;; 单词没有 quote为变量or常量；
;; 单词带有 quote为记号(symbol)；
;; （）没有 quote 为函数调用；
;; （）带有 quote 为列表(list)；
;; (quote ) 函数是把变量or常量变成记号；
;; (list )  函数是把函数调用变成列表。
;; 【总结】 symbol 是‘未成年的’数据， list 是‘未成年的’函数， eval 是催熟剂。


(def a 3)
(eval 'a) ;; => 3

(def ls-plus '(+ 1 2 3 4))
(eval ls-plus)

(quote a) ;; 'a
(list + 1 2 3 4) ;; '(+ 1 2 3 4)

;; 不论是 symbol 还是 list 都带有 【延迟计算】 的味道，何时强制其计算呢？ 【eval】
;; 不仅如此， list 还可以被嵌套在其他函数中作为变量使用（好像是一句废话，list当
;; 然可以传递给函数，但当你意识到你可以用 list 的 api 来处理一个未成年的函数时就
;; 不会觉得这是废话了）。此时就像 infinite seq 一样，无线集合不计算，只有通过
;; take 时才会产生实际集合。 list 被嵌套进其他函数时，整个函数就变成一个更大的
;; list（好像也是一句废话，但当你意识到一个未成年人和一个成年人在一起，成年人也
;; 变成未成年，就不会觉得这时废话了），不调用 eval 时，他将永远是一个 list， 只
;; 有在调用 eval 时，bonnnnng~. 抽象变为现实。计算由此开始。


(def helo (list str (vector "x" "y" "z")))
(def helol '( str [ "x" "y" "z" ]))

(eval (cons apply helo)) ;; 有了 apply， eval 仅仅把 x 作为元素。把 [] 作为 vector，把 “” 作为字符串符号。
;; => "xyz"
(eval helo) ;; 没有 apply 的时候，eval 把每个符号都当成列表元素，包括 [] “ ” 和 x
;; => "[\"x\" \"y\" \"z\"]"

(eval (cons apply helol))
;; => "xyz"
(eval helol)
;; => "[\"x\" \"y\" \"z\"]"


;; 【编程感想：何时必须使用 quote】
;; 如果一个名字，不是已经定义的函数or变量名的其他关键字， 比如 def 或者其他东西，
;; 必须加入 '. 比如 (list 'def 'my-reduce (xxxxx))


;; 【编程感想： associative】
;; map 和 vector 都是 associative 的，所以他们都可以使用 assoc/get
;; list 和 set 都不是 associative 的，所以他们都不能使用 assoc/get


;; DONE【编程感想： list 与 ‘()】
;; 为什么如下两者， 在 repl 中返回的结果不一样。
;; chap7.core> (list str 1 2 3 [1 2 3])
;; (#function[clojure.core/str] 1 2 3 [1 2 3])
;; chap7.core> '(str 1 2 3 [1 2 3])
;; (str 1 2 3 [1 2 3])

;; 在 lisp 方言中， '() 相当于 apply ' to every element inside (). 也就是说，
;; '() 是不对列表元素进行计算的一种 list 构造方法。每个元素都是’未成年的‘变量
;; （evaluate later）。 想对其元素进行计算的方法为加上 ~.

;; (list) 则直接计算参数，而后构造列表。


;; DONE 【编程感想： [] “” 何时是特殊意义符号，何时仅仅是符号】

(def helo (list str (vector "x" "y" "z")))

(eval (cons apply helo)) ;; 有了 apply， eval 仅仅把 x 作为元素。把 [] 作为 vector，把 “” 作为字符串符号。
;; => "xyz"

(eval helo) ;; 没有 apply 的时候，eval 把每个符号都当成列表元素，包括 [] “ ” 和 x
;; => "[\"x\" \"y\" \"z\"]"

;; 因为 (vector "x" "y") => ["a" "b"]

;; (str ["a" "b"]) 时，相当于对 vector 整体做 string 转换，这毫无疑问是 vector
;; 这个类型实现了 string 方法，其中定义了 vector 转换成 string 的格式就是带 "[]"
;; 的形式。

;; (apply str ["a" "b"]) 就是另外一回事了， apply 会将 vector 爆破成单个元素，也
;; 就是此时 str 的参数从 vector 类型变成了字符串类型。那么 str 此时就会进行字符
;; 串拼接操作。

;; 所以造成如上两种方式区别的根本原因是： vector 也实现了 ”字符串化“ 操作，其
;; 格式为 "[e1 e2 e3]"




;; 总结起来说，对于 clojure 等 lisp 方言，与其说你在些代码，不如说你在写编译器抽
;; 象语法树。





;;;;;;;;;;;;;;;;
;; THE READER ;;
;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;
;; 一， read-string    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

;; 在 clojure 中， reading 和 evaluation 是两个独立的过程，你可以通过
;; ~reading-string~ 函数直接使用 Reader 的功能。

(read-string "(+ 1 2)")
;; => (+ 1 2)

(list? (read-string "(+ 1 2)"))
;; => true

(def ps1 (concat [ println ] (read-string "(+ 1 2)")))
;; => (#function[clojure.core/println] + 1 2)
(def ps2 (cons println (read-string "(+ 1 2)")))
;; => (#function[clojure.core/println] + 1 2)
(def ps3 (cons 'println (read-string "(+ 1 2)")))
;; => (#function[clojure.core/println] + 1 2)



(eval ps1)
;; => nil
;; why not 3

(eval ps2)
;; => nil
;; why not 3

(eval ps3)
;; => nil
;; why not 3


;; [ read-string 返回的 list 的特殊性 ]

;; 使用 eval 的时候一定注意，list 的第一个元素会作为函数使用，而之后的所有元素会
;; 作为这个函数的参数传递给这个函数。 如果参数是list 类型，那么在没有 eval 该
;; list 时，他就仅仅是个 list。该函数会将其当成 list 来处理， 而不是将其当成函数。


;; without eval list => list
;; eval + list => function


;; ~(read-string)~ 接受字符串（代表代码文本）作为参数，返回 list 型数据结构（代
;; 表 ADT）。 注意， read-string 返回的 list 是通过 '() 构建的list， 不是通
;; 过 (list) 构建的list。


;; 换言之， read-string 返回的list 的元素都是未经计算的 symbol。



(type (first '(+ - *)))
;; clojure.lang.Symbol
(type (first (list + -)))
;; clojure.core$_PLUS_
(type (first (read-string "(+ 1 2)")))
;; clojure.lang.Symbol



;;;;;;;;;;;;;;;;;;;;;;
;; 二， reader form ;;
;;;;;;;;;;;;;;;;;;;;;;

;; "(+ 1 2)" 就叫做 reader form. 在没有经过 read-string 之前他只是代码文本（或者
;; 叫reader form）， 经过 read-string 之后他就变成了 '() 型list，然后再经过
;; evaluator 发生计算。


;; [ Reader 与 Evaluator 的工作流程 ]

;; 总结： 代码字符串是未成年的 list， list 是未成年的函数。

;;                  reader                      evaluator
;; reader form ------------------> NDS -------------------> value
;;
;; list reader form                list
;; symbol reader form              symbol
;; vector reader form              vector
;; map reader form                 map
;; compact text reader form        unzipped many structures

;; reading 和 evaluation 两个过程是完全独立的， reading 之后你可以对其结果进行计
;; 算， 也可以将该结果传递给其他函数进行再加工，然后再计算，甚至干脆不计算。


;; [ reader form 与 NDS 之间的一对一 ]

;; Reader Form 有很多种类， 每个种类都可以将固定形式的字符串，转为一种 clojure
;; 的内部数据结构。字符串与内部数据结构之间是 【 一对一 】 的映射关系，这种字符
;; 串只能通过 reader 编程这种数据结构。

;; 1. list reader form   ----> list
;; 2. symbol reader form ----> symbol
;; 3. vector reader form ----> vector
;; 4. map reader form    ----> map

(map type (map read-string ["(+ 1 2)" "str" "[1 2 3]" "{:name \"yuanlong\"}"]))
;; => (clojure.lang.PersistentList
;;     clojure.lang.Symbol
;;     clojure.lang.PersistentVector
;;     clojure.lang.PersistentArrayMap)



;; [ 全部一对一么？ ]

;; 如下的 reader form 返回的结果就包含了三个部分。

;; 1. fn* symbol
;; 2. [p1__17955#] vector
;; 3. (+ 1 p1__17955#) list

;; 也就是一个 "#()" 型字符串， 返回了一个同时包含三种内部数据结构的 list。 一对
;; 三？ 且返回的内部类型（通过 type获取） 为 ~clojure.lang.Cons~.

(#(+ 1 %) 3)

(read-string "#(+ 1 %)")
;; => (fn* [p1__17955#] (+ 1 p1__17955#))

(type (read-string "#(+ 1 %)"))
;; => clojure.lang.Cons

;; 这里 reader 使用了某种称之为 “reader macro” 的东西来读取字符串。下面解释。


;;;;;;;;;;;;;;;;;;;
;; READER MACROS ;;
;;;;;;;;;;;;;;;;;;;

;; reader macro 定义了一组如何把字符串转换成特定clojure数据结构的规则。他可以接
;; 受许多内涵特殊意义的字符（比如本例的 "#()" "'()" ) 并将其还原为完整形式的（压
;; 缩与解压缩）。reader macro 在表现形式上大多使用宏字符 --- ' # @ 等。 所以见到
;; ' # @ 基本都会调用 reader macro 去进行转换。

(read-string "'(a b c)")

(read-string "@var")

(read-string ";ignored!\n(+ 1 2)")
;; => (+ 1 2)





;;;;;;;;;;;;;;;;;;;
;; THE EVALUATOR ;;
;;;;;;;;;;;;;;;;;;;



;; clojure 的 evaluator 可以被看成一个接受内部数据结构的函数，该函数会根据数据结
;; 构的类型来对其做相应的处理。

;; 1. 计算一个 list。clojure evaluator 会将第一个元素理解为
;; function/macro/special form, 其他元素则被理解为其值本身。





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; THESE THINGS EVALUATOR TO THEMSELVES ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; 注意我们现在讨论的过程，其输入已经是 reader 过的数据结构，也就是 list symbol
;; 等，是实实在在的 clojure 内部数据结构。

(read-string "(+ 1 2)")
;; => (+ 1 2)

;; 我们现在讨论的就是从 (+ 1 2)----> Evaluator ----> value 的这个过程。


;; clojure evaluator 会把除了 list or symbol 的数据结构都计算为该数据结构本身。
;; 据此可以把 evaluator 的计算对象分成三种：

;; 1. symbol
;; 2. list
;; 3. others



;; [ 1. SYMBOLS -> EVALUATOR ]

;; 程序员需要给 value 绑定 name，这里的 “name” 就是 symbol . 这些 symbol 作为
;; evaluator 的输入，会被 evaluator解析。 evaluator 会遍历你在当前名字空
;; 间(namespace)所有的名值绑定，找到该 symbol 对应的内容。 一个 symbol 会被
;; evaluator 解析成： binding name 或者 special form.

;; 鉴于 evaluator 对 symbol 的方法是多选择的---可以解析为 binding name or
;; special form. 所以解析过程所选择的顺序就只为重要：

;; 1. 是否可以解析为 special form
;; 2. 是否可以解析为 local binding， eg defined by ~let~ or ~function args~
;; 3. 是否可以解析为 ns binding, eg eg defined by ~def~ or ~defn~
;; 4. 以上都不是，则抛出异常




(read-string "+")

(list (read-string "+") 1 2 3)





;; [ 2. LIST -> EVALUATOR ]


;; List 可以衍生三种形式：
;; 1. empty/non-empty value list ---> evaluator 解析该结构为其本身；
;; 2. function ---> 解析该结构为函数调用；
;; 3. special form ---> 按照相应计算规则解析；


;; 为什么将 special form 单独列出来，因为他的解析规则与函数完全不一样：
;; (quote (a b c))上面这个形式，”（a b c）” 如果没有前面的 quote，那么
;; evaluator 会按照值列表或者函数来解析。但有了 quote 之后，解析规则变了。quote
;; 使得 (a b c) 按照 symbol 列表来解析。




;; [ Special Forms ]
;; : quote, def, let, loop, fn, do, recur





;;;;;;;;;;;;
;; Macros ;;
;;;;;;;;;;;;


;; Macro 存在以及好用的原因就在于， 其一，语言的编译或者解释为机器码的过程分为两
;; 个程序员都可控的过程：1. code-text -> reader -> NDS 2. NDS -> evaluator ->
;; value. 其二，NDS 这个编译器的中间产品，与程序员编写的代码是完全一样的。换言之，
;; 现在程序员写好程序有两种方法执行他，其一交给 reader，其二交给 evaluator。



;;             eval 前 transform                   eval
;; input    -------------------> evaluator      --------------> value
;; 任意形式                      特定形式
;; data structures               data structure



;; input 任意格式
(read-string "(1 + 1)")

;; eval 前转换
(def infix-add (let [infix (read-string "(1 + 1)")]
  (list (second infix) (first infix) (last infix))))

;; eval
(eval infix-add)


;; eval前转换这一步，就可以将其理解为 macro。所以 macro 是在做什么，他是在把用户
;; 输入的数据结构整理成 evaluator 需要的数据结构。 macro 是处在 reader 和
;; evaluator 之间的一步。他接受 reader 吐出的数据结构，经过处理再吐出给
;; evaluator 进行计算。


;; 一个 macro 例子

(defmacro ignore-last-operand

  ;; 【编程感想： butlast】
  ;;
  ;;目前处理列表的函数有： first, second, last, butlast, take/take-while,
  ;; drop/drop-while, get, nth
  ;;
  ;; butlast 是函数, 接受 coll ,返回除了最有一个位置的其余元素组成的 coll

  [function-call]
  (butlast function-call))

(ignore-last-operand (+ 1 2 10))

;; 通过上面的例子，可以看到宏的特点：
;; 1. [ 参数 ]不解析
;; 2. [ 返回值 ]自动 eval

;; 注意，宏调用尤其特殊，宏的参数并没有被先计算再传递给宏，而是整体作为 list 进
;; 行传递。 这一点与函数调用非常不同，函数都是先计算参数，而后传递。

;; 宏   --- 参数不计算，直接传递，类似晚解析的概念。
;; 函数 --- 参数先行计算，而后传递。

;; 宏的参数中的 symbol 和 list 都不进行解析直接传递（不计算直接传递的意思就是将
;; 参数当成普普通通的列表、symbol，带入宏体进行列表处理）。也就是 list 的第一个
;; 元素不会被解析为 function, macro, special form.

;; 【宏概念总结】 宏就是不对参数进行解析，直接交给宏体处理，返回值自带 eval 的特
;; 殊语法。


;; The process of determining the return value of a macro is called macro
;; expansion, and you can use the function [ macroexpand ] to see what data
;; structure a macro returns before that data structure is evaluated


;; [ 编程感想： macroexpand ]
;; 
;; macroexpand 会递归的处理所有嵌套的宏，直到他不再是个宏为止。

(macroexpand '(ignore-last-operand (+ 1 2 10)))
;; => (+ 1 2)


(defmacro infix
  [infixed]
  (list (second infixed) (first infixed) (last infixed)))


(infix (1 + 2))

;; 既然【宏】可以把【不规则】形式通过 【 晚解析+自动计算 】转换成【规则】形式，
;; eg (1 + 2)->(+ 1 2)。 也就是说我们甚至可以按照我们希望的结构扩展原有语言的语
;; 法，只要配以相应的 macro expansion 函数即可。 这种做法是lisp 及其方言特有的
;; “魔法” --- syntatic abstranction（句法抽象）。下面介绍：




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SYNTATIC ABSTRACTION and -> MACRO ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 【编程感想： read-string】
;;
;; read-string 接受字符串作为输入，并尝试按其格式将之转换为 clojure 内部的数据结构。
;; 如果无法转为内部数据结构，则统一作为 symbol 看待。
;;
;; read-string is useful for running clojure code from a script or translator.
;; 经常用来从外部读入脚本并运行之。read-string 与 read 一样，通过设置
;; *read-eval* ( 设置为true )可以直接运行读入的内容。所以务必保证读入源头的安全
;; 性。
;;
;;
;;
;;
;; 【编程感想： read 与 read-line】
;;
;; read 是从stream中读入 *下一个* object， 也就是他会按照类似编译原理中学到的内
;; 容，尝试对流中的内容进行识别，找到最 *近* 的一个他认为的 object 然后读入。如
;; 果流中没有内容，他会提示你输入。
;;
;; read-string 与 read 完全一致，都是【下一个】【转为object】，不同的是 read 是
;; 从流中读入， 而 read-string 是从字符串中读入， 所以 read 不需要参数， 而
;; read-string 需要接字符串参数。
;;
;; 如果你输入的是带空格的字符串，那么使用 read 时只会读 *第一个* 空格之前的内容，
;; 并尝试将其转为内部 object然后返回。
;;
;; 此时再尝试使用 read 的时候，就不会再让提示你输入，而是直接从剩下的第一个中读
;; 入。换言之，你输入的内容已经存储在 *in* 指定的那个流中， read 只是尝试读 *下
;; 一个* 而已。
;;
;; chap7.core> (read) ;; <= 用户输入:22 2 2
;; 22 ;;返回只会返回 22， 并且是 java.lang.Long 型
;; chap7.core> (read-line)
;; " 2 2"
;;
;; chap7.core> (read) ;; <= 用户输入:22 2 2
;; 22 ;;返回只会返回 22， 并且是 java.lang.Long 型
;; chap7.core> (read)
;; 2 ;;返回 2， 也是 java.lang.Long 型
;;
;; 当前 ns 使用的 stream 可以通过全局变量 *in* 来获取。
;;
;; read 会尝试进行转型为内部数据结构，read-line 不进行转型，认为都是字符串，这一
;; 点跟 slurp 类似，只不过 read-line 是从流（eg,用户输入）中， slurp 是从文件系
;; 统中读入。此时如果想尝试转为内部数据结构，甚至直接运行的话，还需要接
;; read-string.
;;
;;
;;
;;
;; 【编程感想： clojure 的函数参数计算从右往左】
;; 一般语言，阅读代码，从外到内。
;; clojure 阅读代码，从内到外。
;;
;; 一般语言，函数参数计算，从左到右。
;; clojure 函数参数计算，从右到左。
;;
;; read vs. read-line vs. read-string vs. slurp.
;;
;; read next, from stream  , output object -> read
;; read next, from string  , output object -> read-string
;; read line, from stream  , output string -> read-line
;; read file, from filepath, output string -> slurp

;; read next, 的意思是遇到空格就终止
;; (read-string "[1 2 3]  xxxx")
;; [1 2 3]


;; 【编程感想：关于 c.j.i/resource 为何可以直接从 lein 根目录下的 resource 文件
;; 夹直接读取文件】

;; Leiningen borrows the convention for resources from maven, with slightly
;; different folder layouts. The rule states that the resources folder must be
;; used as a compile time classpath root, meaning that leiningen is right in
;; putting all the files inside resources folder in the root location inside the
;; jar. The problem is that physical location != classpath location, so that
;; while the former changes when you package you application, the latter stays
;; the same (classpath:/) You'd better rely on classpath location to find your
;; file system resources, or take them out of the jar and place them into a
;; predictable folder, either a static or a configurable one.


;; https://stackoverflow.com/a/24208934
;; https://stackoverflow.com/a/8009899

;; 简单说就是， lein 按照 maven 的方式来组织开发和发布的工程classpath配置---开发
;; 阶段(”开发阶段”意为 dev 阶段，该阶段默认 resource 文件夹就是 classpath 根目
;; 录之一，如要更改该设置，可以在工程配置文件 project.clj 的 :profile {:dev
;; {xxxx}} 中配置)中 resource 目录是classpath 的根目录。所以 c.j.i/resource 可以
;; 直接通过文件名访问 resource 目录下的文件。

;; 同时还可以发现，工程根目录下的文件无法通过 c.j.i/resource 直接访问， 这时因为
;; project classpath 与 project filepath 是完全独立的两个概念。classpath 是编译
;; 后 .class 文件的组织路径； filepath 是源代码 .clj 文件的组织路径。“编译过程”会
;; 根据 project.clj 配置对源代码文件组织结构进行调整的。两者不是等价关系，更进一
;; 步说一个是输入一个是输出： (release filepath project.clj) => classpath.

;; 这也是为什么 spit/slurp 可以直接通过只提供文件名就访问工程根目录的文件内容，
;; 而 c.j.i/resource 却不行； 反之， c.j.i/resource 可以直接访问 resource 目录中
;; 的文件内容，而 spit/slurp 却不型。 根本原因是： spit/slurp 是针对工程
;; filepath 的函数， c.j.i/resource 是针对工程 classpath 的函数。

;; chap7/
;;      CHANGELOG.md
;;      doc
;;      .gitignore
;;      .hgignore
;;      LICENSE
;;      .nrepl-port
;;      project.clj
;;      README.md          <--
;;      resources/
;;               hello.txt <--
;;      src/
;;      target/
;;      test/

(slurp "README.md") ;; content can be get
(clojure.java.io/resource "README.md") ;; nil

(slurp "hello.txt" nil) ;; error, file not exist
(clojure.java.io/resource "hello.txt") ;; content can be get



(defn read-resource
  [path]
  (read-string (slurp (clojure.java.io/resource path))))


(defn read-resource
  [path]
  (-> path
      clojure.java.io/resource
      slurp
      read-string))



;;;;;;;;;;;;;;
;; EXERCISE ;;
;;;;;;;;;;;;;;



;; 1. Use the list function, quoting, and read-string to create a list that,
;; when evaluated, prints your first name and your favorite sci-fi movie.

(eval (read-string "(println \"yuanlong\" \" fav:Intersteler\")"))
(eval (list println "yuanlong" " fav:xxx"))


(defmacro infix-x
  [fncall]
  ;; ((second fncall) (first fncall) (last fncall))
  ;;
  ;; 上面这种写法就会导致整个整个宏只是把最后一个参数返回出去。为什么会这样，宏
  ;; 必须采用底层语法，也就是构造的list 就必须使用 list 的完整的正式的构造方法。
  (list (second fncall) (first fncall) (last fncall))
  )

(infix-x (1 + 1))

(defmacro prns-eval
  [maclst]
  (list 'do (println "yiddi " "fav: Intersteler") maclst))
;; (do (+ 1 2) (println "yiddi " "fav: Intersteler"))


(prns-eval (+ 1 2))



;; TODO 2. Create an infix function that takes a list like (1 + 3 * 4 - 5) and
;; transforms it into the lists that Clojure needs in order to correctly
;; evaluate the expression using operator precedence rules.

(compute-macro (1 + 3 * 4 - 5))
;; => (+ 1 (- (* 3 4) 5))
;; => (+ 1 (* 3 (- 4 5)))

(compute-macro (1 + 3 * 4 / 5))
;; => (+ 1 (* 3 (/ 4 5)))

(compute-macro (1 / 3 + 4 - 5))
;; => (- (+ (/ 1 3) 4) 5)

(compute-macro (1 + 3 - 4 * 5))
;; => (+ 1 (- 3 (* 4 5)))

;; defmacro [l oprt r]
;; 1. if oprt = / * , (list (eval (oprt l (1st r))) (remain r))
;; 2. if oprt = + - , (list oprt l (recur (1st r) (2nd r) (remain r)))

(defmacro mcall
  [expr]
  (let [l (first expr)
        oprt (second expr)
        rlst (drop 2 expr)
        rl (first rlst)
        roprt (second rlst)
        rr (drop 2 rlst)]
   (if (= roprt nil)
    (list oprt l rl)
    (if (or (= oprt '/) (= oprt '*))
      ;; (mcall (list (eval (list oprt l rl)) roprt rr)) ;; 貌似 mcall 的参数会先发生计算。
      (mcall `(concat `(list ~(list oprt l rl) roprt) rr)) ;; 貌似 mcall 的参数会先发生计算。
      ;;(list oprt l (mcall (list rl roprt rr))) ;; error wrong number of args passed to mcall
      (list oprt l (mcall `(concat `(list rl roprt) rr)))
      ))))

;; 问(1)： defmacro 中的“自带计算”是过程中每一步都在计算，还是说过程中只进行组
;; 合，组成最终的表达式才发生计算？

;; 问(2)： 是否允许在 defmacro 中使用 eval？

;; 问(3)： 宏定义是否允许递归自调用？

(mcall (1 / 3 - 4 * 5))
;; l 1, oprt /, rlst (3 - 4 * 5), rl 3, roprt -, rr (4 * 5)
;; (mcall (concat (list (/ 1 3) -) (4 * 5)))
;; (mcall (1/3 - 4 * 5))
;; l 1/3, oprt -, rlst (4 * 5), rl 4, roprt *, rr (5)
;; (list - 1/3 (mcall (concat (list 4 *) (5))))
;; (list - 1/3 (mcall (list 4 * 5)))
;; l 4, oprt *, rlst (5), rl 5, roprt nil, rr nil.
;; (list - 1/3 (list * 4 5))



(mcall (1 + 3 - 4 * 5))
;; l 1, oprt /, rlst (3 - 4 * 5), rl 3, roprt -, rr (4 * 5)
;; (list / 1 (mcall (list 3 - ())))




(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))



