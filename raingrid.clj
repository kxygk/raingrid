(ns raingrid
  "Functionality for creating fake IMERG data"
  (:use geoprim)
  (:require fastmath.random))


;; White square
#_
(let [buff (gen-imerg)]
  (-> buff
      .getRaster
      (.setPixels 1000 ;; weird function
                  1000 ;; takes an array of int/double/float
                  200  ;; but the underlying format is UShort
                  200  ;; and seemingly 0-65535
                  (int-array 40000 (* (Short/MAX_VALUE)
                                      2)))) ;; larger numbers get rounded to 65535
  (-> buff
      (javax.imageio.ImageIO/write "tiff"
                                   (java.io.File. "white-square.tiff"))))

;; Note:
;; Default BufferImage values are `0.0` ie. `black`

;; IMERG data is given to us in GeoTIFF format.
;; We inspect the format to see its encoding
#_
(->  "./imerg-sample.tif"
     java.io.File.
     javax.imageio.ImageIO/read
     .toString)
;; => "BufferedImage@a3ca079: type = 11 ColorModel: #pixelBits = 16 numComponents = 1 color space = java.awt.color.ICC_ColorSpace@66971d7a transparency = 1 has alpha = false isAlphaPre = false ShortInterleavedRaster: width = 3600 height = 1800 #numDataElements 1"



;; Setting seeds for random variables is done in this way
#_
(let [seed 69]
  (->> (fastmath.random/distribution :gamma {:rng (fastmath.random/rng :jdk seed)})
       fastmath.random/->seq
       (take 10)))
;; With `nil` the values become "random" again
#_
(let [seed nil]
  (->> (fastmath.random/distribution :gamma {:rng (fastmath.random/rng :jdk seed)})
       fastmath.random/->seq
       (take 10)))


(defn
  gen
  [width
   height
   & [{:keys [background-noise-mean
              pattern-noise-mean
              pattern-noise-var-scaling-fac
              ver-offset
              ver-stride
              ver-stripe-width
              hor-offset
              hor-stride
              hor-stripe-width
              seed]
       :or   {background-noise-mean         5000.0
              pattern-noise-mean            (* Short/MAX_VALUE
                                               0.5)
              pattern-noise-var-scaling-fac 0.2
              ver-offset                    0
              ver-stride                    7
              ver-stripe-width              2
              hor-offset                    0
              hor-stride                    19
              hor-stripe-width              3
              seed                          nil}}]]
  (let [image (java.awt.image.BufferedImage. width
                                             height
                                             java.awt.image.BufferedImage/TYPE_USHORT_GRAY)]
    (let [raster               (-> image
                                   .getRaster)
          background-dist      (fastmath.random/distribution :gamma {:shape 1.0 ;; keep exponential
                                                                     :scale background-noise-mean
                                                                     :rng   (fastmath.random/rng :jdk seed)})
          pattern-dist         (fastmath.random/distribution :normal {:mu  pattern-noise-mean
                                                                      :sd  (* pattern-noise-mean
                                                                              pattern-noise-var-scaling-fac)
                                                                      :rng (fastmath.random/rng :jdk seed)})
          total-field-size     (* width
                                  height)
          ver-white-array-size (* height
                                  ver-stripe-width)
          hor-white-array-size (* width
                                  hor-stripe-width)]
      ;; Fill field with gamma noise
      (.setPixels raster
                  0
                  0 ;; takes an array of int/double/float
                  width  ;; but the underlying format is UShort
                  height  ;; and seemingly 0-65535
                  (int-array total-field-size
                             (->> background-dist
                                  fastmath.random/->seq
                                  (filter pos?))))
      ;; Vertical stripes (gamme + normal)
      (->> (range ver-offset
                  width
                  ver-stride)
           (run! (fn [x-start]
                   (.setPixels raster
                               x-start           ; weird function
                               0                 ; takes an array of int/double/float
                               ver-stripe-width  ; but the underlying format is UShort
                               height            ; and seemingly 0-65535
                               (int-array ver-white-array-size
                                          (map +
                                               (->> pattern-dist
                                                    fastmath.random/->seq
                                                    (filter pos?))
                                               (->> background-dist
                                                    fastmath.random/->seq
                                                    (filter pos?))))))))
      ;; Horizontal stripes
      (->> (range hor-offset
                  height
                  hor-stride)
           (run! (fn [y-start]
                   (.setPixels raster
                               0
                               y-start
                               width
                               hor-stripe-width
                               (int-array hor-white-array-size
                                          (filter #(and (> %
                                                           0.0)
                                                        (> (* Short/MAX_VALUE
                                                              2.0)
                                                           %))
                                                  (map +
                                                       (->> pattern-dist
                                                            fastmath.random/->seq
                                                            (filter pos?))
                                                       (->> background-dist
                                                            fastmath.random/->seq
                                                            (filter pos?))))))))))
    image))


(defn
  spit-rainlike
  "Spit to file based on the pattern noise mean level
  Output images are rather large because I'm not setting any compression
  Original images have LZW compression
  See:
  file:///usr/share/doc/openjdk-21-jre-headless/api/java.desktop/javax/imageio/metadata/doc-files/tiff_metadata.html#Compression
  "
  [pattern-level]
  (-> (gen 3600
           1800
           {:pattern-noise-mean pattern-level
            :seed               pattern-level})
      (javax.imageio.ImageIO/write "tiff"
                                   (java.io.File. (str "/home/kxygk/Projects/raingrid/out/noise"
                                                       (format "%07d"
                                                               pattern-level)
                                                       ".tiff")))))

#_
(spit-rainlike 8000)
#_
(->> (range 10
            9000
            60)
     (run! spit-rainlike))


