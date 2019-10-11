(ns peg-again.core
  (:gen-class))

;;;;;;;;;;;;;;;;;;;
;; data          ;;
;; ====          ;;
;; 1. meta ds    ;;
;; 2. base ds    ;;
;; 3. appearance ;;
;;;;;;;;;;;;;;;;;;;




;; [ META DATA ]: tail of each row compose a vector

;; [ HANDY API ]
;; ordinary version of rt-seq
(defn rt-seq
  ([rn] (rt-seq 0 1 rn))
  ([iv cn rn]
   (let [item (+ iv cn)]
     (if (>= cn rn)
       (cons item nil)
       (cons item (rt-seq item (inc cn) rn))
       ))))
(rt-seq 3) ;; => [1 3 6]


;; iseq version of rt-seq
(defn rt-seq
  ([] (rt-seq 0 1))
  ([iv cn]
   (let [item (+ iv cn)]
     (cons item (lazy-seq (rt-seq item (inc cn)))))))
(take 5 (rt-seq))


;; give a num output the row-num of it
(defn row-num
  [n]
  (inc (count (take-while #(< % n) (rt-seq))))
  )
(row-num 3) ;; => 2



;; give a num output the row-tail of it
(defn row-tail
  [n]
  (last (take (let [rn (row-num n)]
                rn) (rt-seq))))
(row-tail 3)



;; give a number judge row-tail or not
(defn row-tail?
  [n]
  (= n (row-tail n)))
(row-tail? 2)



;; give the row-num output the last val of board
(defn last-val
  [rn]
  (last (take rn (rt-seq))))
(last-val 4)



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; base data must contains 3 infos: ;;
;; 1. rows                          ;;
;; 2. every items                   ;;
;;    2.1 peg or not                ;;
;;    2.2 candidate connect with    ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; [ TRANSFORM API ] 

;; convert from meta data to base data.


;; [ CONNECT ] 

;; give the pos output the candidate connect destination and neighbor of it, as
;; a map
(defn connect
  [b mp p n d]
  (if (> d mp)
    b
    (reduce #(assoc-in %1 [(first %2) :connect (second %2)] n)
            b
            [ [p d] [d p] ])))
(connect {} 15 1 3 5)


;; connect right
(defn c-r
  [b p]
  (let [n (inc p)
        d (inc n)]
    (if (or (row-tail? p) (row-tail? n))
      b
      (connect b (last-val (:rows b)) p n d))))
(c-r {:rows 4} 1)
(c-r {:rows 4} 9)


;; connect down left
(defn c-d-l
  [b p]
  (let [n (+ p (row-num p))
        d (+ n (row-num n))]
    (connect b (last-val (:rows b)) p n d)))
(c-d-l {:rows 4} 4)


;; connect down right
(defn c-d-r
  [b p]
  (let [n (+ 1 p (row-num p))
        d (+ 1 n (row-num n))]
    (connect b (last-val (:rows b)) p n d)))
(c-d-r {:rows 4} 3)

;; connect one
(defn connect-one
  [board pos]
  (reduce #(%2 %1 pos)
          board
          [c-r c-d-l c-d-r]))
(connect-one {:rows 4} 1)

;; connect all
(defn connect-all
  [board]
  (reduce (fn [b p]
            (connect-one b p))
          board
          (range 1 (inc (last-val (:rows board))))))

;; [ PEG ] 

;; get peg by pos
(defn get-peg
  [b p]
  (get-in b [p :pegged]))
(get-peg {:rows 4 1 {:pegged false}} 1)

;; peg one, add or remove
(defn peg-one
  [board pos peg]
  (assoc-in board [pos :pegged] peg))
(peg-one {:rows 4} 1 true)


;; peg all
(defn peg-all
  [board]
  (reduce (fn [b p]
            (peg-one b p true))
          board
          (range 1 (inc (last-val (:rows board))))))
(peg-all {:rows 4})


;; [ INITIALIZE BOARD ] 

;; initial the board
(defn build-bd
  [rn]
  (reduce (fn [b p] (connect-one (peg-one b p true) p))
           (assoc {} :rows rn)
           (range 1 (inc (last-val rn)))))

(build-bd 4)



;; [ JUMP ]

;; jump from postion to destination
(defn jump
  [b p d]
  (let [n (get-in b [p :connect d])]
    (if (and (get-peg b p) (get-peg b n) (not (get-peg b d)) n)
      (peg-one (peg-one (peg-one b p false) n false) d true)
      (do (println "CANNOT JUMP!") b ))))

(jump (peg-one (build-bd 5) 6 false) 1 6)



;; [ CAN MOVE ]

;; whether or not can go move peg
(defn finish?
  [b]
  (some (fn [mpv]
          (some true? (map (fn [cnvec]
                             (let [d (first cnvec)
                                   n (second cnvec)]
                               (and (get-peg b n) (not (get-peg b d)))))
                           (get-in (second mpv) [:connect]))))
        (filter #(get-in (second %) [:pegged]) b)))
(finish? (peg-one (build-bd 5) 6 false))


;; ;; find all pos that pegged true
;; (filter #(get-in (second %) [:pegged]) b)
;; ;; [ [ 1 {:pegged true :connect {}} ] [ 8 {:pegged true :connect {}} ] ]

;; ;; get candidate connection of pos
;; (get-in b [p :connect]) ;; => {4 2 6 3}

;; ;; find condition satisfied inside candidate
;; (map (fn [cnvec]
;;        (let [d (first cnvec)
;;              n (second cnvec)]
;;          (and (get-peg b n) (not (get-peg b d)))))
;;      (get-in (second mpv) [:connect]))

;; ;; one can move
;; (some true? bool-vect)


;; TODO list all peg can moves



;; [ UI ]


;; [ letter vector ]
(def letter-vect (map (comp str char) (range (int \a) (inc (int \z)))))
letter-vect


;; [ print board ]
(defn print-board
  [b]
  (reduce #(str %1 %2 "\n")
          ""
          (map (partial line-str b) (range 1 (inc (:rows b))))))
(println (print-board (peg-one (build-bd 5) 6 false)))



;; map from board to str
(map #(str (nth letter-vect (dec %))
           (if (get-in (peg-one (build-bd 5) 6 false) [ % :pegged ])
             "0"
             "-")) (range 1 (inc (last-val 5))))

;; reduce whole to a map
(defn my-group-by
  [f c]
  (reduce (fn [a [k v]]
            (assoc-in a [k] (into [] (conj (get-in a [k]) v))))
          {}
          (map vector (map f c) c)))

(my-group-by #(mod % 2) [1 2 3 4])

;; group by row-num
(my-group-by #(row-num %) (range 1 (last-val 5)))

;; compute each line char #
(defn lcnum
  [r]
  (+ (* 2 r) (dec r)))

;; compute each line space #
(defn lspcnum
  [r rn]
  (/ (- (lcnum rn) (lcnum r)) 2))

;; line-space compose a str
(defn lsstr
  [r rn]
  (apply str (repeat (lspcnum r rn) " ")))
(lsstr 1 5)

;; get line pos
(defn line-pos
  [r rn]
  (get (my-group-by #(row-num %) (range 1 (inc (last-val rn) ))) r))
(line-pos 6 6)


;; line-letter compose a seq
(defn llseq
  [r rn b]
  (map #(str (nth letter-vect (dec %))
             (if (get-in b [ % :pegged ])
               "0"
               "-")) (line-pos r rn)))
(llseq 5 5 (peg-one (build-bd 5) 6 false))


;; llseq -> llstr
(defn llstr
  [r rn b]
  (clojure.string/join #" " (llseq r rn b)))
(llstr 5 5 (peg-one (build-bd 5) 6 false))


;; print line, line-str
(defn line-str
  [b r]
  (str (lsstr r (:rows b)) (llstr r (:rows b) b)))
(line-str (peg-one (build-bd 5) 6 false) 5)









;; [ Interact with User ]

;; input row #
;; initialize board
;; display
;; input the peg to remove
;; display
;; input the 'p' and 'd'
;; display
;; check finish yes tip restart or end
;; restart goto 'input row #'
;; end (System/exit 0)


;; input row #
;; (println "Input row # [5]")
(defn iptdft [dft] (clojure.string/trim
                    (let [ipt (read-line)] (if (= ipt "") dft ipt))))


;; tip start
(defn tip-rn
  []
  (println "Input row # [5]:")
  (let [rn (Integer. (iptdft "5"))]
    (tip-rmpeg (build-bd rn))))


;; letter -> int
(defn letint [l] (inc (- ((comp int first) l) ((comp int first) "a"))))
(letint "z")


;; display and tip the peg to remove
(defn tip-rmpeg
  [b]
  (println "\nNow the board is:")
  (println (print-board b))
  (println "\nInput the peg to remove[e]:")
  (let [rmpeg (letint (iptdft "e"))]
    (tip-fromto (peg-one b rmpeg false))))


;; tip move peg from to
(defn tip-fromto
  [b]
  (println "\nNow the board is:")
  (println (print-board b))
  (println "\nInput the peg from to:")
  (let [[p d] (map letint (re-seq #"[a-zA-Z]" (iptdft nil)))
        cb (jump b p d)]
    (if (finish? cb)
      (tip-fromto cb)
      (tip-end cb))))

(map letint (re-seq #"[a-zA-Z]" (iptdft nil)))



;; tip end
(defn tip-end
  [b]
  (println "\nNow the board is:")
  (println (print-board b))
  (println "[ r ]estart game or [ e ]nd it:")
  (let [sb (iptdft "r")]
    (if (= "r" sb)
      (tip-rn)
      (System/exit 0))))




(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (tip-rn))
