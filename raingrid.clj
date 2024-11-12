(ns raingrid
  "Functionality for creating fake IMERG data"
  (:use ;;geogrid
   geoprim))

(defn
  gen
  [width
   height
   type
   & [{:keys [ver-offset
              ver-stride
              ver-stripe-width
              hor-offset
              hor-stride
              hor-stripe-width]
       :or   {ver-offset       0
              ver-stride       7
              ver-stripe-width 2
              hor-offset       0
              hor-stride       19
              hor-stripe-width 3}}]]
  (let [image (java.awt.image.BufferedImage. width
                                             height
                                             java.awt.image.BufferedImage/TYPE_USHORT_GRAY)]
    (let [raster               (-> image
                                   .getRaster)
          background-dist      (fastmath.random/distribution :gamma {:shape 1.0
                                                                     :scale 5000.0})
          pattern-dist         (fastmath.random/distribution :normal {:mu (* (Short/MAX_VALUE)
                                                                             0.5) ;; half-max
                                                                      :sd (* (Short/MAX_VALUE)
                                                                             0.1)})
          total-field-size     (* width
                                  height)
          ver-white-array-size (* height
                                  ver-stripe-width)
          hor-white-array-size (* width
                                  hor-stripe-width)]
      ;; Fill field with gamma
      (.setPixels raster
                  0
                  0 ;; takes an array of int/double/float
                  width  ;; but the underlying format is UShort
                  height  ;; and seemingly 0-65535
                  (int-array total-field-size
                             (->> background-dist
                                  fastmath.random/->seq
                                  (filter pos?))))
      ;; Vertical stripes
      (->> (range ver-offset
                  width
                  ver-stride)
           (run! (fn [x-start]
                   (.setPixels raster
                               x-start ;; weird function
                               0 ;; takes an array of int/double/float
                               ver-stripe-width  ;; but the underlying format is UShort
                               height  ;; and seemingly 0-65535
                               (int-array ver-white-array-size
                                          (map +
                                               (->> pattern-dist
                                                    fastmath.random/->seq
                                                    (filter pos?))#_
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
                                          (map +
                                               (->> pattern-dist
                                                    fastmath.random/->seq
                                                    (filter pos?))
                                               (->> background-dist
                                                    fastmath.random/->seq
                                                    (filter pos?)))))))))
    (-> image
        (javax.imageio.ImageIO/write "tiff"
                                     (java.io.File. "vertical-stripes.tiff")))))


;; IMERG data is given to us in GeoTIFF format.
;; We inspect the format to see its encoding
#_
(->  "./imerg-sample.tif"
     java.io.File.
     javax.imageio.ImageIO/read
     .toString)
;; => "BufferedImage@a3ca079: type = 11 ColorModel: #pixelBits = 16 numComponents = 1 color space = java.awt.color.ICC_ColorSpace@66971d7a transparency = 1 has alpha = false isAlphaPre = false ShortInterleavedRaster: width = 3600 height = 1800 #numDataElements 1"

(defn
  gen-imerg
  "Generate a fake image based on the IMERG params"
  []
  (gen 3600
       1800
       java.awt.image.BufferedImage/TYPE_USHORT_GRAY))

#_
(gen-imerg)
;; => #object[java.awt.image.BufferedImage 0x22e7c455 "BufferedImage@22e7c455: type = 11 ColorModel: #pixelBits = 16 numComponents = 1 color space = java.awt.color.ICC_ColorSpace@66971d7a transparency = 1 has alpha = false isAlphaPre = false ShortInterleavedRaster: width = 3600 height = 1800 #numDataElements 1"]


#_
(-> (gen-imerg)
    .getRaster)
;; => #object[sun.awt.image.ShortInterleavedRaster 0x7c4cae87 "ShortInterleavedRaster: width = 3600 height = 1800 #numDataElements 1"]

;; Default BufferImage values are `0.0` ie. `black`
#_
(-> (gen-imerg)
    .getRaster
    (.getPixel 0
               0
               nil))
;; => [0.0]

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




(double-array 10)

#_
(-> (gen-imerg)
    (javax.imageio.ImageIO/write "tiff"
                                 (java.io.File. "test-out.tiff")))

(->  "./imerg-sample.tif"
     java.io.File.
     javax.imageio.ImageIO/read
     (javax.imageio.ImageIO/write "tiff"
                                  (java.io.File. "reincoded.tiff")))
;; Output images are rather large because I'm not setting any compression
;; Original images have LZW compression
;; See:
;; file:///usr/share/doc/openjdk-21-jre-headless/api/java.desktop/javax/imageio/metadata/doc-files/tiff_metadata.html#Compression
