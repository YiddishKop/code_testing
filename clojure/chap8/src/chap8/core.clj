(ns chap8.core
  (:gen-class))

;; 本节过后，应该掌握宏技巧：
;; 1. quote
;; 2. syntax quote
;; 3. unquote
;; 4. unquote splicing
;; 5. gensym

;; 本节过后，应该知道宏危险：
;; 1. double evaluation
;; 2. variable capture
;; 3. macro infection


;; 什么是宏的核心，宏的作用是整理输入格式成为 NDS 格式，所以宏中的每个元素就有两
;; 种角色可以选择：1. 以 ns 赋予的意义协助宏处理格式转换。2. 直接作为代码出现在
;; NDS 中（NDS本身就是代码）。


;; 如果没有加 quote，则该单位被认为是协助宏处理格式转换的单位，该单位会立即被计
;; 算解析(under macro context)，之后再进行语句组合。(f a b), 会分别解析 f, a, b
;; 三个单位，然后组合成 (f a b) 进行运算。


;; 如果加了 quote， 则该单位被认为是直接作为代码出现在 NDS 中，该单位会一直作为
;; 代码直到宏返回时才会计算解析(under ns context)，之后再进行语句组合。 `(f a
;; b), 会分别解析 f, a, b 三个单位，然后组合成 (f a b) 进行运算。



;;;;;;;;;;;;;;;;;;;;;;;;;;
;; MACROS ARE ESSENTIAL ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;



;; if def defn loop recur, these are all special form in clojure. while ~when~
;; is not special form, it's just a macro.



;;;;;;;;;;;;;;;;;;;;;;;;
;; ANATOMY OF A MACRO ;;
;;;;;;;;;;;;;;;;;;;;;;;;



;; The body will almost always return a list.Because macros are a way of
;; transforming a data structure into a form Clojure can evaluate, and Clojure
;; uses lists to represent function calls, special form calls, and macro calls

;; You can use any function, macro, or special form within the macro body, and
;; you call macros just like you would a function or special form.

;; infix macro
(defmacro infix
  [infixed]
  (list (second infixed) (first infixed) (last infixed)))

(infix (1 + 1))

;; macro 可以被看成一个输入为 code，输出亦为 code 的函数。

;; macro:
;; 1. argments dont evaluate.
;; 2. result auto evalulate.
;; 3. return list always.
;; 4. macro support arguments destructuring.
;; 5. macro support multi-arity
;; 6. macro support recursively call


;; 【编程感想：如何使用 macroexpand】
;; 使用 macroexpand 需要注意参数必须是【显式】的列表类型：
;; 应该是： (macroexpand '(infix (1+2))). ;;=> (+ 1 2)
;; 不该是： (macroexpand (infix (1+2))).  ;;=> 3



;; macro 中可以使用参数解析：
(defmacro infix-2
  [[oprd-l op oprd-r]]
  (list op oprd-l oprd-r))

(infix-2 (1 + 3))


;; TODO 同样可以创建 multi-arity macro
(defmacro and
  ([] true)
  ([x] x)
  ([x & next]
   `(let [and# ~x]
      (if and# (and ~@next) and#))))

;; 这里出现了一些特殊的符号，经常与 macro 一起使用，稍后逐一解释：

;; 1. `
;; 2. ~
;; 3. #
;; 4. ~@



;; 【编程感想：multi-arity 实现列表递归的递归基】

;; [Q] 当参数只有一个的时候，递归能够去调用 1-arity body 么？ 如果可以的话，这种
;; 代码结构可以替代 (if ) 的递归终结形式了。

;; [A] 当参数只有一个的时候，递归可以自动识别 1-arity body 并调用之，但是
;; multi-arity 形式[ 无法完全替代 ] if 作为递归基判断， 只能替代列表递归函数，如
;; 果遇到算数运算递归函数就无能为力. 换言之可以替代 (if (empty? ...)) 不能替
;; 代 (if (zero? ...))

;; 处理列表递归问题时， apply 对识别 arity 非常有用。如果参数是列表的话，他不会
;; 被识别为多参数，而是被识别为一个列表参数。所以这时候使用 apply: (apply
;; recur-fn [1 2 3]) => apply 会把 [1 2 3] 炸开，使得参数变为多个。


(defn recadd
  ([] 0)
  ([x] x)
  ([x & rlst]
   (+ x (apply recadd rlst))
   ;; (+ x (recadd rlst)) ;; 不使用 apply，rlst会被识别为单个参数而直接调用 ([x] x)
   ))
(recadd 1 2 3 4)





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Build Lists for Evaluation ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;; For one, you’ll often need to quote expressions to get unevaluated data
;; structures in your final list (we’ll get back to that in a moment). More
;; generally, you’ll need to be extra careful about the difference between a
;; symbol and its value.




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Distinguishing symbols and values ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(let [result expression]
  (println result)
  result)


;; 错误示范
(defmacro my-print-whoopsie
  [expression]
  (list let [result expression]
        (list println result)
        result))


;; 【编程感想： 上面这个代码的三个错误】

;; The code below will tips the error info: Can't take value of a macro:
;; #'clojure.core/let. And the reason is:

;; 1. your macro body tries to get the value that the symbol let refers to,
;; whereas what you actually want to do is return the let symbol itself.

;; 2. you’re trying to get the value of result, which is unbound

;; 3. you’re trying to get the value of println instead of returning its
;; symbol.

;; 一开始我对 result 没绑定这件事有点困惑，但是想一想，result 的值什么时候才能确
;; 定就可以想开了。毫无疑问整个 body 的计算都【不是发生在此时】，【而是发生在彼
;; 刻】，发生在运行时，而现在我们只是在为彼刻运行准备文本。

;; 那为什么 expression 不加quote，因为宏参数在传入时就是以 symbol 的形式传入的。
;; 不可以再加 quote。 quote 符号是可以嵌套的。

;; chap8.core> 1
;; => 1
;; chap8.core> '1
;; => 1
;; chap8.core> ''1
;; => '1
;; chap8.core> '''1
;; => ''1


;; 【编程感想： what quote for macro?】

;; ' quote tells Clojure to turn off evaluation for whatever follows, in this
;; case preventing Clojure from trying to resolve the symbols and instead just
;; returning the symbols. 【The ability to use quoting to turn off evaluation is
;; central to writing macros】.



;; 【编程感想： symbol 解析需要重新复习】

;; TODO 为什么 (type 1) 与 (type '1) 都是Long，后者不应该是 symbol 么。什么解析
;; 规则。

;; why?

;; chap8.core> (type (read-string "1"))
;; java.lang.Long
;; chap8.core> (type (read-string "'1"))
;; clojure.lang.Cons
;; chap8.core> (type (read-string "'1"))
;; clojure.lang.Cons



;; The right way to do this should be:
(defmacro my-print
  [expression]
  (list 'let ['result expression]
        (list 'println 'result)
        'result))

(my-print (+ 1 1))

;; 【编程感想：编写宏的要旨】

;; 时刻记住，编写宏时：

;; 1. 要提供的是【彼刻的代码文本】

;; 2. 代码文本必须是个 【list】

;; 3. 凡是【在彼刻才能/才需要确定的值，才需要启用的函数】都需要使用 quote






;;;;;;;;;;;;;;;;;;;;
;; Simple Quoting ;;
;;;;;;;;;;;;;;;;;;;;

;; 【编程感想： quote 与 ' 与 eval】

;; 似乎直接输入 data structure 本体，默认就是在进行 evaluate，等同于 (eval xxx)
;; 不想 evaluate 就需要加上 quote，(quote xxx)。单引号是 (quote x) 的一个 reader
;; 宏。 'x 基本代表了 (quote x) 的所有功能。

;; without just return value.
(+ 1 2)
;; => 3

;; return an unevaluated data structure.
(quote (+ 1 2))
;; => (+ 1 2)

;; evaluate the "+", return function
+
;; => #function[clojure.core/+]

;; quote the "+", return just symbol
(quote +)
;; => +

;; evaluate unbound symbol, raise an exception:
sweating-to-the-oldies
;; => error

;; quote an unbound symbol, just return the symbol
(quote sweating-to-the-oldies)
;; => sweating-to-the-oldies



;; 大部分时候， Normal Quoting---(quote x) 或者 'x , 在【功能上】已足够应付日常
;; 需求，唯一的问题是【外观上】这种写法容易造成繁琐冗长的代码 ， 这时候需要
;; Syntax Quote. 介绍如下：


;;;;;;;;;;;;;;;;;;;;
;; Syntax Quoting ;;
;;;;;;;;;;;;;;;;;;;;


;; Syntax Quoting 与 Normal Quoting 的不同有两点：

;; 1. syntax quoting will return the fully qualified symbols (that is, with the
;; symbol’s namespace included).

;; 2. syntax quoting allows you to unquote forms using the tilde, ~


;; [ Normal Quoting without a namespace, recursively ]

;; Normal Quoting does not include a namespace if your code doesn’t include a
;; namespace, and if quoting a list will recursively quotes all the elements:

'+
;; => +

'clojure.core/+
;; => clojure.core/+

'(+ 1 2)
;; => (+ 1 2)

;; [ Syntax Quoting with a namespace, recursively ]

;; Syntax quoting a list will syntax quotes all elements

`(+ 1 2)
;; => (clojure.core/+ 1 2)


;; [ Syntax Quoting has ~ to unpuote a form]

`(+ 1 (inc 1))
;; => (clojure.core/+ 1 (clojure.core/inc 1))

`(+ 1 ~(inc 1))
;; => (clojure.core/+ 1 2)

;; 上面的 syntax quoting 如果使用 normal quoting, 等价于：
(list '+ 1 (inc 1)) ;; 也就是说，如果想有针对性的选择表达式的一部分进行 quoting
                    ;; 就必须使用 list。在其中使用 normal quoting, 不加 quoting
                    ;; 的就正常计算。




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Using Syntax Quoting in a Macro ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defmacro code-critic
  [bad good]
  (list 'do
        (list 'println
              "great squid of madrid, this is a bad code:"
              (list 'quote bad))
        (list 'println
              "sweet gorilla of manila, this is a good code:"
              (list 'quote good))))

(code-critic (1 + 1) (+ 1 1))
;; => nil
;; great squid of madrid, this is a bad code: (1 + 1)
;; sweet gorilla of manila, this is a good code: (+ 1 1)




(code-critic (1 + 1) (+ 1 1))
;; => nil
;; great squid of madrid, this is a bad code: (1 + 1)
;; sweet gorilla of manila, this is a good code: (+ 1 1)

;; 使用 'exprl 是希望在宏调用过程中，用该变量的值。如果希望在宏调用过程中，用该
;; 变量的symbol，怎么办。
(defmacro my-code-critic
  [exprl exprr]
  (list 'do
        (list 'println "hello, bad one:" exprl)
        ;; [编程感想：quote 值 与 quote 名]

        ;; (list 'println "hello, bad one:" 'exprl)原来想达到的目标是希望在宏使
        ;; 用过程中，使用该变量的 symbol，而不是计算该变量。但是直接 'exprl 会导
        ;; 致宏在使用过程中尝试解析该 'exprl symbol，【去当前的 ns 中寻找该名字
        ;; 对应的 var or function，或者干脆理解为：在宏中，被 ‘ 标记的单位都会
        ;; 在宏使用时进行 *文本替换*，'println -> println; 没有被 ’ 标记的单位
        ;; 在宏中都会 *直接使用* println -> #function[clojure.core/println]】，
        ;; 结果肯定是找不到的。因为在‘外面’我们没定义过这个变量。所以【 ' 只能
        ;; 帮助宏外定义过的那些symbol 延时启用】，比如外面定义过一个 (def expr
        ;; 1), 这样用可以。

        ;; (list 'println "hello, bad one:" ''exprl)那根据转义字符带来的灵感，使
        ;; 用两个 ' 可以么？ 不可以。因为被 ' 修饰的单位会进行 *文本替换* 也就是
        ;; 说宏使用时， ''exprl -> 'exprl，而 'exprl 会被认为是一个 symbol。直接
        ;; 出现在结果中。

        ;; 所以这里，你是希望得到 exprl 的值（虽然该值是一个 symbol 列表，但是你
        ;; 不需要考虑这个）而不是 exprl 这个名字， 所以第一步仍然是 *直接使用*
        ;; 将该 exprl 名替换为其内容，然后在对该内容进行 symbol。明确这一点非常
        ;; 重要。这种情况通过 'exprl 是做不到的。只能分步处理：1. 先直接使用
        ;; exprl, 2. 然后对其 quote。=> (list 'quote exprl)

        ;; 总结表达的意思是： 如何quote其名直接quote即可，这时其名在宏使用时会替
        ;; 换，如何quote其值，(list 'quote ) 即可。


        ;; 核心问题是，宏调用时，存在内部逻辑（宏整理输入格式成为输出 NDS 格式的
        ;; 逻辑），所以宏中的每个元素就有两种角色可以选择：1. 以 ns 赋予的意义协
        ;; 助宏处理格式转换。2. 直接作为代码出现在 NDS 中。
        (list 'println "hello, good one:" exprr)
        ;; (list 'println "hello, good one:" (list 'quote exprr))
        ;; (list 'println "hello, good one:" exprr)
        ))

(my-code-critic (+ 1 1) (+ 1 1))

(do (println "hello, bad one: " '(1 + 1)) (println "hello, good one:" '(+ 1 1)))


(defmacro my-print
  "
  [编程感想：]

  刚才的程序对比这里的，为什么之前的参数必须使用 (list 'quote exprl), 而这里的内
  部变量可以直接使用 'result ?

  1. 宏参数就是在调用时（未来）才会计算的，本身不需要 quote，也是在之后使用，他
  是宏中待确定的变量；其他都是常量，不加quote 直接翻译，加了 quote 未来翻译。

  2. 宏内的局部变量，不论如何都必须 quote，因为不quote 当下就会翻译，而当下他作
  为一个变量是非法的，类似空指针，只有名字没有值。没法翻译。


  宏，有三个基本问题：

  1. 变量有无外部绑定，
  2. 变量是否自用逻辑，
  3. 参数是否需要运行计算
  4. 内部参数与 vector


  所以三个问题，需要高清：

  1. 对于外部变量，未来翻译，还是现在翻译

  2. 对于宏参数（可以看成代码），由于没有外部绑定，没法在未来使用。换言之，只能
  直接用。宏的参数是代码，如果不加任何处理，参数会在使用宏的时候计算运行，运行方
  式参照在 repl 中输入的模式。宏的参数是以代码的形式进入宏，以及在宏内参与运算，
  这个过程都不会运行（计算），直到宏进行返回时，才会发生计算。

  如下这种方式，如果直接使用 result, result 是没有绑定值的，类似java空指针异常。
  因为 (list 'let [result expression]) 中的 [result expression] 是个 vector 而不
  是内部赋值语句。所以 result 与 expression 是独立的，何时会被当做赋值语句呢？
  宏被调用时。

  "
  [expression]
  (list 'let ['result expression]
        (list 'println 'result)
        'result))







;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Refactoring a Macro and Unquote Splicing ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn criticize-code
  "

  tilda 开头的都是直接使用。 其他的都是调用时从外部引用。

  "
  [criticism code]
  `(println ~criticism (quote ~code)))


(defmacro code-critic1
  [bad good]
  `(do ~(criticize-code "cursed xxxx, bad: " bad)
       ~(criticize-code "sweet xxxx, good: " good)))

(code-critic1 (1 + 2) (+ 1 2))

(defmacro code-critic2
  [bad good]
  `(do ~(map #(apply criticize-code %)
             [["bad one: " bad] ["good: " good]])))

(code-critic2 (1 + 2) (+ 1 2))
;; error

;; 【编程感想： 所有在 `() 内的元素都要注意其危险性】
;;
;; 需要注意，所有在 `() 内的元素都是要在宏调用时引用外部元素的， 那些被 ～ 修饰
;; 的单位会直接计算，但是计算完成后要注意括号的问题，也就是说如果 ～ 修饰的部分
;; 的结果是一个 list。他还是会按照函数来看待这个 list。
;;
;; eg. `(do ~(xxx)) => `(do (1 2 3)) => 此时返回会将 (1 2 3) 看成是某种函数调用，
;; 类似 (g x y) 的形式： g=1; x=2; y=3. Long can not be cast to IFn. 还会报错。








;;;;;;;;;;;;;;;;;;;;;;
;; Unquote Splicing ;;
;;;;;;;;;;;;;;;;;;;;;;

;; Unquote splicing unwraps a seqable data structure, placing its contents
;; directly within the enclosing syntax-quoted data structure

;; 那该如何处理才能将 `(do (1 2 3)) 变成 `(do 1 2 3) 的形式呢， ~@， 名为：
;; unquote splicing. Unquote Splicing = 即时计算 + 拆包， 相当于 (apply ~())

;; 使用 Unquote Splicing 重新实现 code-critic 函数。
(defmacro code-critic3
  [{:keys [good bad]}]
  `(do ~@(map #(apply criticize-code %)
              [["sweet, bad code:" bad] ["omg! good code: " good]])))

(code-critic3 {:bad (1 + 1) :good (+ 1 1) })

(macroexpand '(code-critic3 {:bad (1 + 1) :good (+ 1 1) }))
;; => (do
;;     (clojure.core/println "sweet, bad code:" '(1 + 1))
;;     (clojure.core/println "omg! good code: " '(+ 1 1)))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; THINGS TO WATCH OUT FOR ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; 这里介绍一些宏比较古怪和缺陷的地方，以及如何避免他们。

;; 【编程感想： 使用宏时传递给其的参数，被当成什么】

;; chap8.core> (code-critic3 {:bad (1 + 1) :good (+ 1 1) })
;; => 传给宏就没有问题。
;; chap8.core> {:good (+ 1 1) :bad (1 + 1)}
;; => 单独使用这个参数直接报错。

;; 由此可见， 你传给宏什么实际参数其实无所谓，他都会当成文本（代码）。


;; [ Variable Capture ]

;; 变量捕捉发生在宏内部有局部变量的时候。比如，如下代码：

(def message "Good Job!")
(defmacro with-mischief
  [& stuff-to-do]
  (concat (list 'let ['message "On, big deal!"]) stuff-to-do))

(with-mischief (println "here's how i feel about that thing you do: " message))

(macroexpand-1 '(with-mischief (println "hello")))
;; => (let [message "On, big deal!"] (println "hello"))

(macroexpand '(with-mischief (println "hello")))
;; => (let* [message "On, big deal!"] (println "hello"))

(macroexpand-1 '(with-mischief (println "hello" message)))
;; => (let [message "On, big deal!"] (println "hello" message))

(macroexpand '(with-mischief (println "hello" message)))
;; => (let* [message "On, big deal!"] (println "hello" message))


;; => here's how i feel about that thing you do: On, big deal! 这里很神奇的是，
;; [ 有点外部注入内部的味道 ]， 外部并米有 message 这个变量， 但是宏内部有声明过
;; 这个局部变量（而且是整体作为返回 list 的一部分，也就是并非宏内部逻辑，而是返
;; 回值的一部分。）。这时候该 message 还是会油值。这种情况，容易发生 “误伤”。
;; 如果外部定义了一个 message 变量： (def message "hello,meesage") 会在内部被覆
;; 盖掉。因为内部定义的 message 是局部变量，优先级更高。这有违初衷。


;; 而且可以注意到，上面的 with-mischief 函数没有使用 syntax quoting, 如果使用的
;; 话会出现异常：

(def message "good job!!!!")
(defmacro with-mischief
  [& stuff-to-do]
  `(let [hello "Oh, big deal!"] ~@stuff-to-do)
  ;; `(let [message "Oh, big deal!"] stuff-to-do)
  )
;; 这里为什么没有爆破？

(with-mischief (println "here's how i feel about that thing you do: " message))
;; => 
(with-mischief (println "here's how i feel about that thing you do: "))
;; => 

;; 【总结：什么是 variable capture】
;;
;; 满足如下三点：
;;
;; 1. def 声明了一个【全局变量】， (def message "love")
;;
;; 2. macro 里面声明了一个【同名局部变量】，并作为 list 一部分返回了 `(let [message "hate"])
;;
;; 3. 调用宏时入参带【有该变量】。



;; 【编程感想：为什么 syntax quoting 可以避免这种情况】
;;
;; 只要出现 【局部变量】+【宏调用传入同名变量】clojure 就认为这存在某种”误会“：
;; 宏使用者想使用全局变量，但是他不知道宏定义者在宏内部也定义了一个同名局部变量。


;; ns  macro
;; |
;; |
;;  \
;;   \
;;    \
;;     \
;;      \
;;       |
;;       |
;;       |
;;       |
;;       |
;;      /
;;     /
;;    /
;;   /
;;  /
;; |
;; |
;; |
;; |




;;;;;;;;;;;;
;; gensym ;;
;;;;;;;;;;;;


;; 如果确实想使用 let bindins （局部变量），可以考虑使用 ~gensym~. gensym 函数可
;; 以用来在后续的调用中【产生唯一的 symbol】。一般用在宏的返回list带有 let的语句
;; 的时候，用于声明独立于外部（namespace vars）的内部局部变量，防止 variable
;; capture by inside macro.


;; 【编程感想：let bindings 与 gensym】
;; 一般的 let 在macro 中会报错， 为什么会报错呢。(以代码 (m) 为例)，因为当宏完成
;; 使命返回到 ns 空间中， x 在成为 let 语句块的一部分之前，无法在 ns 空间中解析，
;; 此时就会报错。

(defmacro m [] `(let [x 1] x))
(m) ;; raise ERROR

;; 如何告诉 clojure，在 ns 中请把 ‘x’ 作为 symbol 使用？ gensym.
(defmacro m [] `(let [x# 1] x#))
(m)




;; 可以不带参数
(gensym)
;; => G__18130
(gensym)
;; => G__18133
(gensym)
;; => G__18136

;; 也可以带参数, 参数需为 symbol
(gensym 'message)
;; => message18139




;; 【编程感想： 关于参数 & xxx 的意义】
;;
;; 之前并没有特别在意这个东西，觉得就是声明多个参数时候用的。但其实 [ & xxx ] 表
;; 示:
;;
;; 1. 该函数可以接受多个参数
;;
;; 2. 该函数会把多个入参【框起来】赋值给 xxx
;;
;; eg:
;; (defn margs [x & xs] [x xs])
;; (margs 1 2 3 4)
;; 此时函数内部会做： xs <= [2 3 4], 把多个参数【框起来】赋值给 xs
;;
;; 当你的参数是个列表时，会发生什么？
;; 函数内部依旧会把这个列表【框起来】--- 变成嵌套列表
;;
;; 这里的 'stuff-to-do' 就是这样, 如果你传给他的是 (println "xxx" message), 也就
;; 是传给他一个列表， stuff-to-do <= ((println "xxx" message)), 所以这时候必须使
;; 用 ~@ 爆破掉外面的 ’（）‘

;; gensym , syntax quote, unquote splicing
(defmacro without-mischief
  [& stuff-to-do]
  (let [macro-message (gensym 'message)]
    `(let [~macro-message "Oh, big deal!"]
       ~@stuff-to-do;; 注意理解这里为什么使用 ~@
       (println "I still need to say: " ~macro-message))))


;; auto-gensym , syntax quote, unquote splicing
(defmacro without-mischief
  [& stuff-to-do]
  `(let [message# "Oh, big deal!"]
     ~@stuff-to-do
     (println "I still need to say: " message#)))


;; list without gensym, syntax quote and unquote splicing
(defmacro wit
  [& stuff-to-do]
  (list 'let ['x "Oh, big deal!"]
        (list 'println (list 'quote stuff-to-do))
        (list 'println "I still need to say: " 'x)))


(def message "YUANLONG")
(without-mischief (println "here's how i feel that thing you did: " message))
(macroexpand '(without-mischief (println "here's how i feel that thing you did: " message)))
;; => (let*
;;     [message18520 "Oh, big deal!"]
;;     (println "here's how i feel that thing you did: " message)
;;     (clojure.core/println "I still need to say: " message18520))



(macroexpand '(wit + 1 1))
;; => (let*
;;     [x "Oh, big deal!"]
;;     (println '(+ 1 1))
;;     (println "I still need to say: " x))

;; gensym---generate new symbol 就像 quote 一样，有自己的简写宏形式，只不过quote
;; 的是前缀，gensym 是后缀：
;;
;; quote  -----> '___ -----> `___
;;
;; gensym -----> ___#

;; suffix form of gensym:
`(blarg# blarg#)
;; => (blarg__18165__auto__ blarg__18165__auto__)







;;;;;;;;;;;;;;;;;;;;;;;
;; Double Evaluation ;;
;;;;;;;;;;;;;;;;;;;;;;;

;; 另一个使用宏时应该注意的问题是：宏的入参被重复计算。

(defmacro report
  [to-try]
  `(if ~to-try ;; 1st evaluation
     (println (quote ~to-try) ;; no evaluation
              "was success: "
              ~to-try ;; 2nd evaluation
              )
     (println (quote ~to-try) "was NOT success: " ~to-try)))


;; 这个程序可以证明 to-try 被计算了两次，因为 sleep 了两秒钟。书上说这是个大问题，
;; 如果你的应用对时间要求很高（eg. 银行转账，股票交易，天气预测）这种重复计算会
;; 浪费很多时间。
(macroexpand '(report (do (Thread/sleep 1000) (+ 1 1))))
;; => (if
;;     (do (Thread/sleep 1000) (+ 1 1))
;;     (clojure.core/println
;;      '(do (Thread/sleep 1000) (+ 1 1))
;;      "was success: "
;;      (do (Thread/sleep 1000) (+ 1 1)))
;;     (clojure.core/println
;;      '(do (Thread/sleep 1000) (+ 1 1))
;;      "was NOT success: "
;;      (do (Thread/sleep 1000) (+ 1 1))))


;; how to avoid duplicate evaluation
(defmacro reprot
  [to-try]
  `(let [result# ~to-try]
     (if result#
       (println (quote ~to-try) "was successful" result#)
       (println (quote ~to-try) "was NOT successful" result#))))


;; 虽然再 macroexpand 的时候， let 这个语句不会展示，但在宏中，这种用法就是标准
;; 的返回 let NDS代码 用法。
(macroexpand '(report (do (Thread/sleep 1000) (+ 1 1))))
;; => (if
;;     (do (Thread/sleep 1000) (+ 1 1))
;;     (clojure.core/println
;;      '(do (Thread/sleep 1000) (+ 1 1))
;;      "was success: "
;;      (do (Thread/sleep 1000) (+ 1 1)))
;;     (clojure.core/println
;;      '(do (Thread/sleep 1000) (+ 1 1))
;;      "was NOT success: "
;;      (do (Thread/sleep 1000) (+ 1 1))))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Macro All The Way Down ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; One subtle pitfall of using macros is that you can end up having to write
;; more and more of them to get anything done. This is a consequence of the fact
;; that macro expansion happens before evaluation.


;; good to use the macro 'report' defined below individually
(report (= 1 1))
(report (= 1 2))

;; bad to use with other function, like doseq:
(doseq [code ['(= 1 1) '(= 2 1)]]
  (report code))

(macroexpand '(doseq [code ['(= 1 1) '(= 2 1)]]
                (report code)))
;; => (loop*
;;     [seq_18593
;;      (clojure.core/seq ['(= 1 1) '(= 2 1)])
;;      chunk_18594
;;      nil
;;      count_18595
;;      0
;;      i_18596
;;      0]
;;     (if
;;      (clojure.core/< i_18596 count_18595)
;;      (clojure.core/let
;;       [code (.nth chunk_18594 i_18596)]
;;       (do (report code))
;;       (recur
;;        seq_18593
;;        chunk_18594
;;        count_18595
;;        (clojure.core/unchecked-inc i_18596)))
;;      (clojure.core/when-let
;;       [seq_18593 (clojure.core/seq seq_18593)]
;;       (if
;;        (clojure.core/chunked-seq? seq_18593)
;;        (clojure.core/let
;;         [c__5983__auto__ (clojure.core/chunk-first seq_18593)]
;;         (recur
;;          (clojure.core/chunk-rest seq_18593)
;;          c__5983__auto__
;;          (clojure.core/int (clojure.core/count c__5983__auto__))
;;          (clojure.core/int 0)))
;;        (clojure.core/let
;;         [code (clojure.core/first seq_18593)]
;;         (do (report code))
;;         (recur (clojure.core/next seq_18593) nil 0 0))))))


















;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; map 参数类型析构专题总结 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; map析构对下面集中数据结构有效：
;; 1.clojure原生的hash-map、array-map,以及记录类型
;; 2.任何实现了java.util.Map的对象
;; 3.get方法所支持的任何对象。

;; clojure提供 :keys、:strs、:syms来指定map中key的类型。
;; :keys表示key的类型是关键字；
;; :strs表示key的类型是字符串；
;; :syms表示key的类型是符号。

;; list/vector 集合类型一般是通过【位置】来解析参数，比如：

;; eg. 当参数为 [ [a b] ] 表示接受 vector/list （map/set由于有独自的析构格式这里
;; 不可用）类型参数，并解析前两个元素

;; map 集合没法通过位置来解析，只能通过【键】来解析参数，比如：

;; eg. 当参数为 [ {:keys [a b] }] 表示接受 map 类型参数，读取 map 中的 【键:a 的
;; 值赋值给 a, 键:b 的值赋值给 b】, 如果给的键不是关键字(eg, :a)，而是字符串呢。
;; 这种方法解析不出任何值， a 和 b 都是nil。 这时候需要使用 [ {:strs [a b]} ]

(defn xmap1 [{:keys [a b]}] (do (println a b) [a b]))
(xmap1 {:a 1 :b 2 :c 3})
;; => [1 2]
(xmap1 {"a" 1 "b" 2 :c 3})
;; => [nil nil]

(defn xmap2 [{:strs [a b]}] (do (println a b) [a b]))
(xmap2 {"a" 1 "b" 2 :c 3})
;; => [1 2]

;; TODO :syms 留待以后，暂时明白 syms 什么意思。
(defn xmap3 [{:syms [a b]}] (do (println a b) [a b]))

;; map 结构的普通参数解析的语法格式：被赋【值】的变量放在前面，索引的【键】放在后面

;; [{ {nm :name ag :age} :info}], 表示从传入的 map 结构中找到 :info 的内容，再进
;; 去找到 :name 的值将其赋给内部变量 nm， :age 的值赋给内部变量 ag.

(defn xmap4 [{ {nm :name ag :age} :info}] [nm ag])
(xmap4 {:info {:name "yuanlong" :age 32} :body "body"})
;; => ["yuanlong" 32]



;; 参数析构时使用的 :as 关键字，用来将传入的map 参数整体重新命名，并获得其赋
;; 值。:as 不仅可以用在 map 参数析构，也可以用在其他集合参数析构中，用法一致。

(defn xmap5 [{ {nm :name ag :age} :info :as whole}] [nm ag whole])

(xmap5 {:info {:name "yuanlong" :age 32} :body "body"})
;; => ["yuanlong" 32 {:info {:name "yuanlong", :age 32}, :body "body"}]


(defn xvect2 [ [ a & rst :as all ] ] [a rst all])
(xvect2 [1 2 3 4])
;; => [1 (2 3 4) [1 2 3 4]]
;;     - ------- ---------
;;     a    rst    all





;; 三、接受map参数,为解构参数设置默认值

;; TODO 接受map参数,为解构参数设置默认值

(defn f11[pm  {:keys [key value] :or {key "def-key" value "def-value"}}]
  (println "pm->" pm)
  (println "key->" key)
  (println "value->" value)
)

;; user=> (f11 "abc" {:key "kk" :value 1})
;; pm-> abc
;; key-> kk
;; value-> 1
;; nil
;; user=> (f11 "abc" {:key "k2"})
;; pm-> abc
;; key-> k2
;; value-> def-value
;; nil
;; user=> (f11 "abc" {})
;; pm-> abc
;; key-> def-key
;; value-> def-value
;; nil
;; user=>



(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

