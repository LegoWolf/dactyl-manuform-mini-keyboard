(ns dactyl-keyboard.dactyl
  (:refer-clojure :exclude [use import])
  (:require [clojure.core.matrix :refer [array matrix mmul]]
            [scad-clj.scad :refer :all]
            [scad-clj.model :refer :all]
            [unicode-math.core :refer :all]))

(defn deg2rad [degrees]
  (* (/ degrees 180) pi))

;;;;;;;;;;;;;;;;;;;;;;
;; Shape parameters ;;
;;;;;;;;;;;;;;;;;;;;;;

(def nrows 5)
(def ncols 6)

(def α (/ π 12))                        ; curvature of the columns
(def β (/ π 36))                        ; curvature of the rows
(def centerrow (- nrows 3))             ; controls front-back tilt
(def centercol 2)                       ; controls left-right tilt / tenting (higher number is more tenting)
(def tenting-angle (/ π 12))            ; or, change this for more precise tenting control
(def column-style
  (if (> nrows 5) :orthographic :standard))  ; options include :standard, :orthographic, and :fixed
; (def column-style :fixed)
(def pinky-15u true)

(defn column-offset [column] (cond
                               (= column 2) [0 2.82 -2.5]
                               (>= column 4) [0 -12 5.64]            ; original [0 -5.8 5.64]
                               :else [0 0 0]))

(def thumb-offsets [6 -3 7])

(def keyboard-z-offset 12)               ; controls overall height; original=9 with centercol=3; use 16 for centercol=2

(def extra-width 2.5)                   ; extra space between the base of keys; original= 2
(def extra-height 1.0)                  ; original= 0.5

(def wall-z-offset -5)                 ; original=-15 length of the first downward-sloping part of the wall (negative)
(def wall-xy-offset 5)                  ; offset in the x and/or y direction for the first downward-sloping part of the wall (negative)
(def wall-thickness 2)                  ; wall thickness parameter; originally 5

;; Settings for column-style == :fixed
;; The defaults roughly match Maltron settings
;;   http://patentimages.storage.googleapis.com/EP0219944A2/imgf0002.png
;; Fixed-z overrides the z portion of the column ofsets above.
;; NOTE: THIS DOESN'T WORK QUITE LIKE I'D HOPED.
(def fixed-angles [(deg2rad 10) (deg2rad 10) 0 0 0 (deg2rad -15) (deg2rad -15)])
(def fixed-x [-41.5 -22.5 0 20.3 41.4 65.5 89.6])  ; relative to the middle finger
(def fixed-z [12.1    8.3 0  5   10.7 14.5 17.5])
(def fixed-tenting (deg2rad 0))

; If you use Cherry MX or Gateron switches, this can be turned on.
; If you use other switches such as Kailh, you should set this as false
(def create-side-nubs? true)

;;;;;;;;;;;;;;;;;;;;;;;
;; General variables ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def lastrow (dec nrows))
(def cornerrow (dec lastrow))
(def lastcol (dec ncols))

;;;;;;;;;;;;;;;;;
;; Switch Hole ;;
;;;;;;;;;;;;;;;;;

(def keyswitch-height 14.2) ;; Was 14.1, then 14.25
(def keyswitch-width 14.2)

(def sa-profile-key-height 12.7)

(def plate-thickness 2)
(def side-nub-thickness 4)
(def retention-tab-thickness 1.5)
(def retention-tab-hole-thickness (- plate-thickness retention-tab-thickness))
(def mount-width (+ keyswitch-width 3))
(def mount-height (+ keyswitch-height 3))

(def single-plate
  (let [top-wall (->> (cube (+ keyswitch-width 3) 1.5 plate-thickness)
                      (translate [0
                                  (+ (/ 1.5 2) (/ keyswitch-height 2))
                                  (/ plate-thickness 2)]))
        left-wall (->> (cube 1.5 (+ keyswitch-height 3) plate-thickness)
                       (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                   0
                                   (/ plate-thickness 2)]))
        side-nub (->> (binding [*fn* 30] (cylinder 1 2.75))
                      (rotate (/ π 2) [1 0 0])
                      (translate [(+ (/ keyswitch-width 2)) 0 1])
                      (hull (->> (cube 1.5 2.75 side-nub-thickness)
                                 (translate [(+ (/ 1.5 2) (/ keyswitch-width 2))
                                             0
                                             (/ side-nub-thickness 2)])))
                      (translate [0 0 (- plate-thickness side-nub-thickness)]))
        plate-half (union top-wall left-wall (if create-side-nubs? (with-fn 100 side-nub)))
        top-nub (->> (cube 5 5 retention-tab-hole-thickness)
                     (translate [(+ (/ keyswitch-width 2)) 0 (/ retention-tab-hole-thickness 2)]))
        top-nub-pair (union top-nub
                            (->> top-nub
                                 (mirror [1 0 0])
                                 (mirror [0 1 0])))]
    (difference
     (union plate-half
            (->> plate-half
                 (mirror [1 0 0])
                 (mirror [0 1 0])))
     (->>
      top-nub-pair
      (rotate (/ π 2) [0 0 1])))))

;;;;;;;;;;;;;;;;
;; SA Keycaps ;;
;;;;;;;;;;;;;;;;

(def sa-length 18.25)
(def sa-double-length 37.5)
(def sa-cap {1 (let [bl2 (/ 18.5 2)
                     m (/ 17 2)
                     key-cap (hull (->> (polygon [[bl2 bl2] [bl2 (- bl2)] [(- bl2) (- bl2)] [(- bl2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[m m] [m (- m)] [(- m) (- m)] [(- m) m]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 6]))
                                   (->> (polygon [[6 6] [6 -6] [-6 -6] [-6 6]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [220/255 163/255 163/255 1])))
             2 (let [bl2 sa-length
                     bw2 (/ 18.25 2)
                     key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 0.05]))
                                   (->> (polygon [[6 16] [6 -16] [-6 -16] [-6 16]])
                                        (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                        (translate [0 0 12])))]
                 (->> key-cap
                      (translate [0 0 (+ 5 plate-thickness)])
                      (color [127/255 159/255 127/255 1])))
             1.5 (let [bl2 (/ 18.25 2)
                       bw2 (/ 27.94 2)
                       key-cap (hull (->> (polygon [[bw2 bl2] [bw2 (- bl2)] [(- bw2) (- bl2)] [(- bw2) bl2]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 0.05]))
                                     (->> (polygon [[11 6] [-11 6] [-11 -6] [11 -6]])
                                          (extrude-linear {:height 0.1 :twist 0 :convexity 0})
                                          (translate [0 0 12])))]
                   (->> key-cap
                        (translate [0 0 (+ 5 plate-thickness)])
                        (color [240/255 223/255 175/255 1])))})

;;;;;;;;;;;;;;;;;;;;;;;;;
;; Placement Functions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(def columns (range 0 ncols))
(def rows (range 0 nrows))

(def cap-top-height (+ plate-thickness sa-profile-key-height))
(def row-radius (+ (/ (/ (+ mount-height extra-height) 2)
                      (Math/sin (/ α 2)))
                   cap-top-height))
(def column-radius (+ (/ (/ (+ mount-width extra-width) 2)
                         (Math/sin (/ β 2)))
                      cap-top-height))
(def column-x-delta (+ -1 (- (* column-radius (Math/sin β)))))

(defn offset-for-column [col]
  (if (and (true? pinky-15u) (= col lastcol)) 5.5 0))
(defn apply-key-geometry [translate-fn rotate-x-fn rotate-y-fn column row shape]
  (let [column-angle (* β (- centercol column))
        placed-shape (->> shape
                          (translate-fn [(offset-for-column column) 0 (- row-radius)])
                          (rotate-x-fn  (* α (- centerrow row)))
                          (translate-fn [0 0 row-radius])
                          (translate-fn [0 0 (- column-radius)])
                          (rotate-y-fn  column-angle)
                          (translate-fn [0 0 column-radius])
                          (translate-fn (column-offset column)))
        column-z-delta (* column-radius (- 1 (Math/cos column-angle)))
        placed-shape-ortho (->> shape
                                (translate-fn [0 0 (- row-radius)])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 row-radius])
                                (rotate-y-fn  column-angle)
                                (translate-fn [(- (* (- column centercol) column-x-delta)) 0 column-z-delta])
                                (translate-fn (column-offset column)))
        placed-shape-fixed (->> shape
                                (rotate-y-fn  (nth fixed-angles column))
                                (translate-fn [(nth fixed-x column) 0 (nth fixed-z column)])
                                (translate-fn [0 0 (- (+ row-radius (nth fixed-z column)))])
                                (rotate-x-fn  (* α (- centerrow row)))
                                (translate-fn [0 0 (+ row-radius (nth fixed-z column))])
                                (rotate-y-fn  fixed-tenting)
                                (translate-fn [0 (second (column-offset column)) 0]))]
    (->> (case column-style
           :orthographic placed-shape-ortho
           :fixed        placed-shape-fixed
           placed-shape)
         (rotate-y-fn  tenting-angle)
         (translate-fn [0 0 keyboard-z-offset]))))

(defn key-place [column row shape]
  (apply-key-geometry translate
                      (fn [angle obj] (rotate angle [1 0 0] obj))
                      (fn [angle obj] (rotate angle [0 1 0] obj))
                      column row shape))

(defn rotate-around-x [angle position]
  (mmul
   [[1 0 0]
    [0 (Math/cos angle) (- (Math/sin angle))]
    [0 (Math/sin angle)    (Math/cos angle)]]
   position))

(defn rotate-around-y [angle position]
  (mmul
   [[(Math/cos angle)     0 (Math/sin angle)]
    [0                    1 0]
    [(- (Math/sin angle)) 0 (Math/cos angle)]]
   position))

(defn key-position [column row position]
  (apply-key-geometry (partial map +) rotate-around-x rotate-around-y column row position))

(def key-holes
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> single-plate
                (key-place column row)))))

(def caps
  (apply union
         (for [column columns
               row rows
               :when (or (.contains [2 3] column)
                         (not= row lastrow))]
           (->> (sa-cap (if (and (true? pinky-15u) (= column lastcol)) 1.5 1))
                (key-place column row)))))

;;;;;;;;;;;;;;;;;;;;
;; Web Connectors ;;
;;;;;;;;;;;;;;;;;;;;

(def web-thickness 2)
(def post-size 0.1)
(def web-post (->> (cube post-size post-size web-thickness)
                   (translate [0 0 (+ (/ web-thickness -2)
                                      plate-thickness)])))

(def post-adj (/ post-size 2))
(def web-post-tr (translate [(- (/ mount-width 2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height 2) post-adj) 0] web-post))
(def web-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def web-post-br (translate [(- (/ mount-width 2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))


; wide posts for 1.5u keys in the main cluster

(if (true? pinky-15u)
  (do (def wide-post-tr (translate [(- (/ mount-width 1.2) post-adj)  (- (/ mount-height  2) post-adj) 0] web-post))
      (def wide-post-tl (translate [(+ (/ mount-width -1.2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
      (def wide-post-bl (translate [(+ (/ mount-width -1.2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
      (def wide-post-br (translate [(- (/ mount-width 1.2) post-adj)  (+ (/ mount-height -2) post-adj) 0] web-post)))
  (do (def wide-post-tr web-post-tr)
      (def wide-post-tl web-post-tl)
      (def wide-post-bl web-post-bl)
      (def wide-post-br web-post-br)))

(defn triangle-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (partition 3 1 shapes))))

(defn fan-hulls [& shapes]
  (apply union
         (map (partial apply hull)
              (map (fn [x] (conj x (first shapes))) (partition 1 (rest shapes))))))

(def connectors
  (apply union
         (concat
          ;; Row connections
          (for [column (range 0 (dec ncols))
                row (range 0 lastrow)]
            (triangle-hulls
             (key-place (inc column) row web-post-tl)
             (key-place column row web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place column row web-post-br)))

          ;; Column connections
          (for [column columns
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-bl)
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tl)
             (key-place column (inc row) web-post-tr)))

          ;; Diagonal connections
          (for [column (range 0 (dec ncols))
                row (range 0 cornerrow)]
            (triangle-hulls
             (key-place column row web-post-br)
             (key-place column (inc row) web-post-tr)
             (key-place (inc column) row web-post-bl)
             (key-place (inc column) (inc row) web-post-tl))))))

;;;;;;;;;;;;
;; Thumbs ;;
;;;;;;;;;;;;

(def thumborigin
  (map + (key-position 1 cornerrow [(/ mount-width 2) (- (/ mount-height 2)) 0])
       thumb-offsets))

(defn thumb-tr-place [shape]
  (->> shape
       (rotate (deg2rad  14) [1 0 0])
       (rotate (deg2rad -15) [0 1 0])
       (rotate (deg2rad  10) [0 0 1]) ; original 10
       (translate thumborigin)
       (translate [-15 -10 5]))) ; original 1.5u  (translate [-12 -16 3])
(defn thumb-tl-place [shape]
  (->> shape
       (rotate (deg2rad  10) [1 0 0])
       (rotate (deg2rad -23) [0 1 0])
       (rotate (deg2rad  25) [0 0 1]) ; original 10
       (translate thumborigin)
       (translate [-35 -16 -2]))) ; original 1.5u (translate [-32 -15 -2])))


(defn thumb-mr-place [shape]
  (->> shape
       (rotate (deg2rad  10) [1 0 0])
       (rotate (deg2rad -23) [0 1 0])
       (rotate (deg2rad  25) [0 0 1])
       (translate thumborigin)
       (translate [-23 -34 -6])))
(defn thumb-br-place [shape]
  (->> shape
       (rotate (deg2rad   6) [1 0 0])
       (rotate (deg2rad -34) [0 1 0])
       (rotate (deg2rad  35) [0 0 1])
       (translate thumborigin)
       (translate [-39 -43 -16])))
(defn thumb-bl-place [shape]
  (->> shape
       (rotate (deg2rad   6) [1 0 0])
       (rotate (deg2rad -32) [0 1 0])
       (rotate (deg2rad  35) [0 0 1])
       (translate thumborigin)
       (translate [-51 -25 -11.5]))) ;        (translate [-51 -25 -12])))


(defn thumb-1x-layout [shape]
  (union
   (thumb-mr-place shape)
   (thumb-br-place shape)
   (thumb-tl-place shape)
   (thumb-bl-place shape)))

(defn thumb-15x-layout [shape]
  (union
   (thumb-tr-place shape)))

(def larger-plate
  (let [plate-height (- (/ (- sa-double-length mount-height) 3) 0.5)
        top-plate (->> (cube mount-width plate-height web-thickness)
                       (translate [0 (/ (+ plate-height mount-height) 2)
                                   (- plate-thickness (/ web-thickness 2))]))]
    (union top-plate (mirror [0 1 0] top-plate))))

(def thumbcaps
  (union
   (thumb-1x-layout (sa-cap 1))
   (thumb-15x-layout (rotate (/ π 2) [0 0 1] (sa-cap 1)))))

(def thumb
  (union
   (thumb-1x-layout single-plate)
   (thumb-15x-layout single-plate)
  ; (thumb-15x-layout larger-plate)
))

(def thumb-post-tr (translate [(- (/ mount-width 2) post-adj)  (- (/ mount-height  2) post-adj) 0] web-post))
(def thumb-post-tl (translate [(+ (/ mount-width -2) post-adj) (- (/ mount-height  2) post-adj) 0] web-post))
(def thumb-post-bl (translate [(+ (/ mount-width -2) post-adj) (+ (/ mount-height -2) post-adj) 0] web-post))
(def thumb-post-br (translate [(- (/ mount-width 2) post-adj)  (+ (/ mount-height -2) post-adj) 0] web-post))

; David's modification: these are extruded versions of the thumb-post-* primitives. We use them for thumb-tr-place
; throughout to thicken the problematically thin walls around it, which are caused by especially acute angles.
(def outer-extra-width 1)
(def thumb-post-tr-outer (union thumb-post-tr (translate [   outer-extra-width     outer-extra-width  0] thumb-post-tr)))
(def thumb-post-tl-outer (union thumb-post-tl (translate [(- outer-extra-width)    outer-extra-width  0] thumb-post-tl)))
(def thumb-post-bl-outer (union thumb-post-bl (translate [(- outer-extra-width) (- outer-extra-width) 0] thumb-post-bl)))
(def thumb-post-br-outer (union thumb-post-br (translate [   outer-extra-width  (- outer-extra-width) 0] thumb-post-br)))

(def thumb-connectors
  (union
   (triangle-hulls    ; top two
    (thumb-tl-place web-post-tr)
    (thumb-tl-place web-post-br)
    (thumb-tr-place thumb-post-tl-outer)
    (thumb-tr-place thumb-post-bl-outer))
   (triangle-hulls    ; bottom two
    (thumb-br-place web-post-tr)
    (thumb-br-place web-post-br)
    (thumb-mr-place web-post-tl)
    (thumb-mr-place web-post-bl))
   (triangle-hulls
    (thumb-mr-place web-post-tr)
    (thumb-mr-place web-post-br)
    (thumb-tr-place thumb-post-br-outer))
   (triangle-hulls    ; between top row and bottom row
    (thumb-br-place web-post-tl)
    (thumb-bl-place web-post-bl)
    (thumb-br-place web-post-tr)
    (thumb-bl-place web-post-br)
    (thumb-mr-place web-post-tl)
    (thumb-tl-place web-post-bl)
    (thumb-mr-place web-post-tr)
    (thumb-tl-place web-post-br)
    (thumb-tr-place thumb-post-bl-outer)
    (thumb-mr-place web-post-tr)
    (thumb-tr-place thumb-post-br-outer))
   (triangle-hulls    ; top two to the middle two, starting on the left
    (thumb-tl-place web-post-tl)
    (thumb-bl-place web-post-tr)
    (thumb-tl-place web-post-bl)
    (thumb-bl-place web-post-br)
    (thumb-mr-place web-post-tr)
    (thumb-tl-place web-post-bl)
    (thumb-tl-place web-post-br)
    (thumb-mr-place web-post-tr))
   (triangle-hulls    ; top two to the main keyboard, starting on the left
    (thumb-tl-place web-post-tl)
    (key-place 0 cornerrow web-post-bl)
    (thumb-tl-place web-post-tr)
    (key-place 0 cornerrow web-post-br)
    (thumb-tr-place thumb-post-tl-outer)
    (key-place 1 cornerrow web-post-bl)
    (thumb-tr-place thumb-post-tr-outer)
    (key-place 1 cornerrow web-post-br)
    (key-place 2 lastrow web-post-tl)
    (key-place 2 lastrow web-post-bl)
    (thumb-tr-place thumb-post-tr-outer)
    (key-place 2 lastrow web-post-bl)
    (thumb-tr-place thumb-post-br-outer)
    (key-place 2 lastrow web-post-br)
    (key-place 3 lastrow web-post-bl)
    (key-place 2 lastrow web-post-tr)
    (key-place 3 lastrow web-post-tl)
    (key-place 3 cornerrow web-post-bl)
    (key-place 3 lastrow web-post-tr)
    (key-place 3 cornerrow web-post-br)
    (key-place 4 cornerrow web-post-bl))
   (triangle-hulls
    (key-place 1 cornerrow web-post-br)
    (key-place 2 lastrow web-post-tl)
    (key-place 2 cornerrow web-post-bl)
    (key-place 2 lastrow web-post-tr)
    (key-place 2 cornerrow web-post-br)
    (key-place 3 cornerrow web-post-bl))
   (triangle-hulls
    (key-place 3 lastrow web-post-tr)
    (key-place 3 lastrow web-post-br)
    (key-place 3 lastrow web-post-tr)
    (key-place 4 cornerrow web-post-bl))))

;;;;;;;;;;
;; Case ;;
;;;;;;;;;;

(defn bottom [height p]
  (->> (project p)
       (extrude-linear {:height height :twist 0 :convexity 0})
       (translate [0 0 (- (/ height 2) 10)])))

(defn bottom-hull [& p]
  (hull p (bottom 0.001 p)))

(def left-wall-x-offset 5) ; original 10
(def left-wall-z-offset  3) ; original 3

(defn left-key-position [row direction]
  (map - (key-position 0 row [(* mount-width -0.5) (* direction mount-height 0.5) 0]) [left-wall-x-offset 0 left-wall-z-offset]))

(defn left-key-place [row direction shape]
  (translate (left-key-position row direction) shape))

(defn wall-locate1 [dx dy] [(* dx wall-thickness) (* dy wall-thickness) -1])
(defn wall-locate2 [dx dy] [(* dx wall-xy-offset) (* dy wall-xy-offset) wall-z-offset])
(defn wall-locate3 [dx dy] [(* dx (+ wall-xy-offset wall-thickness)) (* dy (+ wall-xy-offset wall-thickness)) wall-z-offset])

(defn wall-brace [place1 dx1 dy1 post1 place2 dx2 dy2 post2]
  (union
   (hull
    (place1 post1)
    (place1 (translate (wall-locate1 dx1 dy1) post1))
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 post2)
    (place2 (translate (wall-locate1 dx2 dy2) post2))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))
   (bottom-hull
    (place1 (translate (wall-locate2 dx1 dy1) post1))
    (place1 (translate (wall-locate3 dx1 dy1) post1))
    (place2 (translate (wall-locate2 dx2 dy2) post2))
    (place2 (translate (wall-locate3 dx2 dy2) post2)))))

(defn key-wall-brace [x1 y1 dx1 dy1 post1 x2 y2 dx2 dy2 post2]
  (wall-brace (partial key-place x1 y1) dx1 dy1 post1
              (partial key-place x2 y2) dx2 dy2 post2))

(def right-wall
  (let [tr (if (true? pinky-15u) wide-post-tr web-post-tr)
        br (if (true? pinky-15u) wide-post-br web-post-br)]
    (union (key-wall-brace lastcol 0 0 1 tr lastcol 0 1 0 tr)
           (for [y (range 0 lastrow)] (key-wall-brace lastcol y 1 0 tr lastcol y 1 0 br))
           (for [y (range 1 lastrow)] (key-wall-brace lastcol (dec y) 1 0 br lastcol y 1 0 tr))
           (key-wall-brace lastcol cornerrow 0 -1 br lastcol cornerrow 1 0 br))))

(def case-wall-list
  (concat
   (list right-wall)
   ; ; back wall
   (for [x (range 0 ncols)] (key-wall-brace x 0 0 1 web-post-tl x       0 0 1 web-post-tr))
   (for [x (range 1 ncols)] (key-wall-brace x 0 0 1 web-post-tl (dec x) 0 0 1 web-post-tr))

   ; ; left wall
   (for [y (range 0 lastrow)] (union (wall-brace (partial left-key-place y 1)       -1 0 web-post (partial left-key-place y -1) -1 0 web-post)
                                     (hull (key-place 0 y web-post-tl)
                                           (key-place 0 y web-post-bl)
                                           (left-key-place y  1 web-post)
                                           (left-key-place y -1 web-post))))
   (for [y (range 1 lastrow)] (union (wall-brace (partial left-key-place (dec y) -1) -1 0 web-post (partial left-key-place y  1) -1 0 web-post)
                                     (hull (key-place 0 y       web-post-tl)
                                           (key-place 0 (dec y) web-post-bl)
                                           (left-key-place y        1 web-post)
                                           (left-key-place (dec y) -1 web-post))))
   (list
		   (wall-brace (partial key-place 0 0) 0 1 web-post-tl (partial left-key-place 0 1) 0 1 web-post)
		   (wall-brace (partial left-key-place 0 1) 0 1 web-post (partial left-key-place 0 1) -1 0 web-post))
   ; front wall
   (list
		   (key-wall-brace 3 lastrow   0 -1 web-post-bl 3 lastrow 0.5 -1 web-post-br)
		   (key-wall-brace 3 lastrow 0.5 -1 web-post-br 4 cornerrow 0.5 -1 web-post-bl))
   (for [x (range 4 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl x       cornerrow 0 -1 web-post-br)) ; TODO fix extra wall
   (for [x (range 5 ncols)] (key-wall-brace x cornerrow 0 -1 web-post-bl (dec x) cornerrow 0 -1 web-post-br))
   ; thumb walls
   (list
		   (wall-brace thumb-mr-place  0 -1 web-post-br thumb-tr-place  0 -1 thumb-post-br-outer)
		   (wall-brace thumb-mr-place  0 -1 web-post-br thumb-mr-place  0 -1 web-post-bl)
		   (wall-brace thumb-br-place  0 -1 web-post-br thumb-br-place  0 -1 web-post-bl)
		   (wall-brace thumb-bl-place  0  1 web-post-tr thumb-bl-place  0  1 web-post-tl)
		   (wall-brace thumb-br-place -1  0 web-post-tl thumb-br-place -1  0 web-post-bl)
		   (wall-brace thumb-bl-place -1  0 web-post-tl thumb-bl-place -1  0 web-post-bl)
		   ; thumb corners
		   (wall-brace thumb-br-place -1  0 web-post-bl thumb-br-place  0 -1 web-post-bl)
		   (wall-brace thumb-bl-place -1  0 web-post-tl thumb-bl-place  0  1 web-post-tl)
		   ; thumb tweeners
		   (wall-brace thumb-mr-place  0 -1 web-post-bl thumb-br-place  0 -1 web-post-br)
		   (wall-brace thumb-bl-place -1  0 web-post-bl thumb-br-place -1  0 web-post-tl)
		   (wall-brace thumb-tr-place  0 -1 thumb-post-br-outer (partial key-place 3 lastrow)  0 -1 web-post-bl)
		   ; clunky bit on the top left thumb connection  (normal connectors don't work well)
		   (bottom-hull
		    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
		    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
		    (thumb-bl-place (translate (wall-locate2 -0.3 1) web-post-tr))
		    (thumb-bl-place (translate (wall-locate3 -0.3 1) web-post-tr)))
		   (hull
		    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
		    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
		    (thumb-bl-place (translate (wall-locate2 -0.3 1) web-post-tr))
		    (thumb-bl-place (translate (wall-locate3 -0.3 1) web-post-tr))
		    (thumb-tl-place web-post-tl))
		   (hull
		    (left-key-place cornerrow -1 web-post)
		    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
		    (left-key-place cornerrow -1 (translate (wall-locate2 -1 0) web-post))
		    (left-key-place cornerrow -1 (translate (wall-locate3 -1 0) web-post))
		    (thumb-tl-place web-post-tl))
		   (hull
		    (left-key-place cornerrow -1 web-post)
		    (left-key-place cornerrow -1 (translate (wall-locate1 -1 0) web-post))
		    (key-place 0 cornerrow web-post-bl)
		    (thumb-tl-place web-post-tl))
		   (hull
		    (thumb-bl-place web-post-tr)
		    (thumb-bl-place (translate (wall-locate1 -0.3 1) web-post-tr))
		    (thumb-bl-place (translate (wall-locate2 -0.3 1) web-post-tr))
		    (thumb-bl-place (translate (wall-locate3 -0.3 1) web-post-tr))
		    (thumb-tl-place web-post-tl)))
))

(def pinky-wall-list
  (list
   (key-wall-brace lastcol cornerrow 0 -1 web-post-br lastcol cornerrow 0 -1 wide-post-br)
   (key-wall-brace lastcol 0 0 1 web-post-tr lastcol 0 0 1 wide-post-tr)))
(def pinky-walls (union pinky-wall-list))

(def case-walls (apply union case-wall-list))
(def case-walls-plate
		(let [walls (map cut (concat (list (cube 1 1 1)) case-wall-list pinky-wall-list))]
				(apply fan-hulls walls)))
(def case-walls-plate-outline
		(let [walls (map cut (concat case-wall-list pinky-wall-list))]
				(apply union walls)))

(def usb-hole-ref (key-position 0 0 (map - (wall-locate2  0  -1) [0 (/ mount-height 2) 0])))
(defn usb-hole-position [position] (map + position [(first usb-hole-ref) (second usb-hole-ref) 3]))
(def usb-hole-position-left (usb-hole-position [22 19.3 0]))
(def usb-hole-position-right (usb-hole-position [23 19.3 0]))
(defn usb-hole [position] (union
		; hole for the jack
		(translate (map + position [0 10 3]) (cube 9.5 20 4))
		; groove for the board
		(translate (map + position [0 (- 0 wall-thickness 0.5) 1]) (cube 26 13 2.1))))
(defn usb-holder [position] (translate (map + [0 (- 0 wall-thickness 2) -0.5] position) (cube 8 15.5 5)))
(def usb-hole-left (usb-hole usb-hole-position-left))
(def usb-hole-right (usb-hole usb-hole-position-right))
(def usb-holder-left (usb-holder usb-hole-position-left))
(def usb-holder-right (usb-holder usb-hole-position-right))

(def pro-micro-position (map + (key-position 0 1 (wall-locate3 -1 0)) [-6 2 -15]))
(def pro-micro-space-size [4 10 12]) ; z has no wall;
(def pro-micro-wall-thickness 2)
(def pro-micro-holder-size [(+ pro-micro-wall-thickness (first pro-micro-space-size)) (+ pro-micro-wall-thickness (second pro-micro-space-size)) (last pro-micro-space-size)])
(def pro-micro-space
  (->> (cube (first pro-micro-space-size) (second pro-micro-space-size) (last pro-micro-space-size))
       (translate [(- (first pro-micro-position) (/ pro-micro-wall-thickness 2)) (- (second pro-micro-position) (/ pro-micro-wall-thickness 2)) (last pro-micro-position)])))
(def pro-micro-holder
  (difference
   (->> (cube (first pro-micro-holder-size) (second pro-micro-holder-size) (last pro-micro-holder-size))
        (translate [(first pro-micro-position) (second pro-micro-position) (last pro-micro-position)]))
   pro-micro-space))

(def trrs-hole-board-size [2.25 13.5 20]) ; trrs board groove
(def trrs-hole-board-shift [3.5 -1.75 -2])
(def trrs-hole-jackrect-size [5.5 13.5 6.5]) ; trrs box groove surrounding jack cylinder hole
(def trrs-hole-jackrect-shift [0 -1.75 0])
(def trrs-hole-position-left (map + usb-hole-position-left [-18.6 0 -2]))
(def trrs-hole-position-right (map + usb-hole-position-right [-16 0 -2]))
(def trrs-holder-size [2 13 5]) ; holder tray for trrs board
(def trrs-holder-shift [4.75 0 7])
(def trrs-holder-thickness 2)
(def trrs-holder-thickness-2x (* 2 trrs-holder-thickness))
(defn trrs-hole-cylinder-position [position]
		[(first position)
		(+ (second position) (/ trrs-holder-thickness -2))
		(+ (last position) 3 (/ (+ (last trrs-holder-size) trrs-holder-thickness) 2))])
(defn trrs-rotate [position angle shape]
		(let [p (trrs-hole-cylinder-position position)]
				(->> shape (translate (map - p)) (rotate (deg2rad angle) [0 1 0]) (translate p))))
(defn trrs-holder [position angle mirror?]
		(trrs-rotate position angle
				(union
						(->>
								(cube (+ (first trrs-holder-size) trrs-holder-thickness) (+ (second trrs-holder-size) trrs-holder-thickness-2x) (last trrs-holder-size))
								(translate trrs-holder-shift)
								((fn [x] (if mirror? (mirror [-1 0 0] x) x)))
								(translate [
										(first position)
										(- (second position) wall-thickness trrs-holder-thickness)
										(+ (last position) (/ (+ (last trrs-holder-size) trrs-holder-thickness) 2))])))))
(defn trrs-hole [position angle mirror?]
		(trrs-rotate position angle
				(union
						; trrs jack cylinder hole
						(->> (->> (binding [*fn* 30] (cylinder 2.75 20)))
								(rotate (deg2rad 90) [1 0 0])
								((fn [x] (if mirror? (mirror [-1 0 0] x) x)))
								(translate (trrs-hole-cylinder-position position)))
						; trrs box groove surrounding jack cylinder hole
						(->> (apply cube trrs-hole-jackrect-size)
								(translate trrs-hole-jackrect-shift)
								((fn [x] (if mirror? (mirror [-1 0 0] x) x)))
								(translate (trrs-hole-cylinder-position position)))
						; trrs board groove
						(->> (apply cube trrs-hole-board-size)
								(translate trrs-hole-board-shift)
								((fn [x] (if mirror? (mirror [-1 0 0] x) x)))
								(translate [
										(first position)
										(+ (second position) (/ trrs-holder-thickness -2))
										(+ (last position) (/ (last trrs-hole-board-size) 2) trrs-holder-thickness)])))))
(def trrs-hole-left (trrs-hole trrs-hole-position-left -11.5 false))
(def trrs-hole-right (trrs-hole trrs-hole-position-right 0 true))
(def trrs-holder-left (trrs-holder trrs-hole-position-left -11.5 false))
(def trrs-holder-right (trrs-holder trrs-hole-position-right 0 true))

(defn screw-insert-shape [bottom-radius top-radius height]
  (union
   (->> (binding [*fn* 30]
          (cylinder [bottom-radius top-radius] height)))
   (translate [0 0 (/ height -2)] (->> (binding [*fn* 30] (sphere top-radius))))))

(defn screw-countersink-shape [bottom-radius top-radius height]
		(let [sink-height 1.7]
   (->> (binding [*fn* 30]
   		(union
   				(translate [0 0 (/ (- height sink-height) 2)] (cylinder [bottom-radius top-radius] sink-height))
   				(translate [0 0 (/ sink-height -2)] (cylinder [bottom-radius bottom-radius] (- height sink-height -0.01)))
   		)))))

(defn screw-insert [shape column row bottom-radius top-radius height offset]
  (let [shift-right   (= column lastcol)
        shift-left    (= column 0)
        shift-up      (and (not (or shift-right shift-left)) (= row 0))
        shift-down    (and (not (or shift-right shift-left)) (>= row lastrow))
        position      (if shift-up     (key-position column row (map + (wall-locate2  0  1) [0 (/ mount-height 2) 0]))
                          (if shift-down  (key-position column row (map - (wall-locate2  0 -1) [0 (/ mount-height 2) 0]))
                              (if shift-left (map + (left-key-position row 0) (wall-locate3 -1 0))
                                  (key-position column row (map + (wall-locate2  1  0) [(/ mount-width 2) 0 0])))))]
    (->> (shape bottom-radius top-radius height)
         (translate (map + offset [(first position) (second position) (/ height 2)])))))

(defn screw-insert-all-shapes [shape bottom-radius top-radius height]
  (union (screw-insert shape 0 0         bottom-radius top-radius height [11 10 0])
         (screw-insert shape 0 lastrow   bottom-radius top-radius height [0 0 0])
        ;  (screw-insert shape lastcol lastrow  bottom-radius top-radius height [-5 13 0])
        ;  (screw-insert shape lastcol 0         bottom-radius top-radius height [-3 6 0])
         (screw-insert shape lastcol lastrow  bottom-radius top-radius height [0 12 0])
         (screw-insert shape lastcol 0         bottom-radius top-radius height [0 7 0])
         (screw-insert shape 1 lastrow         bottom-radius top-radius height [0 -16 0])))

; Hole Depth Y: 4.4
(def screw-insert-height 7)
(def screw-countersink-height 2.2)

; Hole Diameter C: 4.1-4.4
(def screw-insert-bottom-radius (/ 5.3 2))
(def screw-insert-top-radius (/ 5.3 2))
(def screw-insert-holes  (screw-insert-all-shapes screw-insert-shape screw-insert-bottom-radius screw-insert-top-radius screw-insert-height))

; Wall Thickness W:\t1.65
(def screw-insert-outers (screw-insert-all-shapes screw-insert-shape (+ screw-insert-bottom-radius 1.65) (+ screw-insert-top-radius 1.65) (+ screw-insert-height 1.5)))
(def screw-insert-screw-holes  (screw-insert-all-shapes screw-insert-shape 1.7 1.7 350))
(def screw-insert-screw-countersinks  (screw-insert-all-shapes screw-countersink-shape 3.2 1.7 screw-countersink-height))

(def pinky-connectors
  (apply union
         (concat
          ;; Row connections
          (for [row (range 0 lastrow)]
            (triangle-hulls
             (key-place lastcol row web-post-tr)
             (key-place lastcol row wide-post-tr)
             (key-place lastcol row web-post-br)
             (key-place lastcol row wide-post-br)))

          ;; Column connections
          (for [row (range 0 cornerrow)]
            (triangle-hulls
             (key-place lastcol row web-post-br)
             (key-place lastcol row wide-post-br)
             (key-place lastcol (inc row) web-post-tr)
             (key-place lastcol (inc row) wide-post-tr)))
          ;;
)))

(def model-base (difference
                  (union
                   key-holes
                   pinky-connectors
                   pinky-walls
                   connectors
                   thumb
                   thumb-connectors
                   (difference (union case-walls
                                      screw-insert-outers
                                      pro-micro-holder
                                      )
                               screw-insert-holes))
                  (translate [0 0 -20] (cube 350 350 40))))

(def model-right
		(difference
				(union
						model-base
						trrs-holder-right
	     usb-holder-right
				)
				(union
						usb-hole-right
						trrs-hole-right
		)))

(def model-left
		(difference
				(union
						model-base
						trrs-holder-left
	     usb-holder-left
				)
				(union
						usb-hole-left
						trrs-hole-left
				)))

(def plate-base
		(let [extra-thickness (max 1 (- (+ screw-countersink-height 1) plate-thickness))]
    (translate [0 0 (/ plate-thickness 2)]
      (difference
      		(union
        		(extrude-linear {:height plate-thickness} case-walls-plate)
        		(translate [0 0 (/ (+ plate-thickness extra-thickness) 2)]
        				(extrude-linear {:height extra-thickness} (union case-walls-plate-outline (cut screw-insert-outers))))
        )
        (union
        		(translate [0 0 -10] screw-insert-screw-holes)
        		(translate [0 0 (- (/ plate-thickness -2) 0.01)] screw-insert-screw-countersinks)
        )))))

(spit "things/right.scad"
      (write-scad model-right))

(spit "things/left.scad"
      (write-scad (mirror [-1 0 0] model-left)))

(spit "things/right-test.scad"
      (write-scad
       (difference
        (union
         key-holes
         pinky-connectors
         pinky-walls
         connectors
         thumb
         thumb-connectors
         case-walls
         thumbcaps
         caps)

        (translate [0 0 -20] (cube 350 350 40)))))

(spit "things/right-plate.scad"
      (write-scad plate-base))

(spit "things/left-plate.scad"
      (write-scad (mirror [-1 0 0] plate-base)))

(spit "things/test.scad"
      (write-scad
       (difference trrs-holder-right trrs-hole-right)))

(defn -main [dum] 1)  ; dummy to make it easier to batch
