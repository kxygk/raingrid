(ns raingrid
  "Functionality for creating fake IMERG data"
  (:use ;;geogrid
   geoprim))

(defn
  gen
  [^long width
   ^long height
   ^long type]
  (let [image (java.awt.image.BufferedImage. width
                                             height
                                             java.awt.image.BufferedImage/TYPE_USHORT_GRAY)]
    image))



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
