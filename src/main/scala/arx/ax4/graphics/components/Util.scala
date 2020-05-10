package arx.ax4.graphics.components

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}

import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.mathutil.RunningVector
import arx.core.vec.{ReadVec2f, Vec2f, Vec2i, Vec4i}
import arx.core.vec.coordinates.{AxialVec, CartVec}
import arx.graphics.Image

object Util {

  def posWithinHex(rawCenter : CartVec, relative : ReadVec2f, const : AxDrawingConstants) : ReadVec2f = {
    rawCenter.asPixels(const.HexSize) + Vec2f(relative.x * const.HexSize, relative.y * const.HexHeight)
  }


  object HexMaskGenerator {
    def main(args: Array[String]): Unit = {
      val img = Image.withDimensions(125, 109)

      img.setPixelsFromFunc((x,y) => {
        if (AxialVec.fromCartesian(Vec2f(x - img.width / 2, y - img.height / 2), (img.width-1).toFloat) == AxialVec(0,0)) {
          Vec4i(255, 255, 255, 255)
        } else {
          Vec4i(0, 0, 0, 255)
        }
      })

      Image.save(img, "/tmp/hex.png")
    }

    def maskImageToHex(img : Image) = {
      val runningVec = new RunningVector
      img.transformPixelsByFunc((x,y, c) => {
        if (AxialVec.fromCartesian(Vec2f(x - img.width / 2, y - img.height / 2), (img.width-1).toFloat) == AxialVec(0,0)) {
          runningVec.value(x, y)
          c
        } else {
          Vec4i(c.r, c.g, c.b, 0)
        }
      })
      img.cropped(Vec2i(runningVec.min.xy), Vec2i(runningVec.max.xy))
      img
    }
  }


  object Downsizer {
    def main(args: Array[String]): Unit = {
      val output = Paths.get("/Users/nvt/Code/Ax4/src/main/resources/third-party/zeshioModified")
      val root = Paths.get("/Users/nvt/Code/Ax4/src/main/resources/third-party/ZeshiosPixelHexTileset1.1_HexKit/")
      Files.walkFileTree(root, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
          if (file.toString.endsWith("png") && ! file.toString.contains("upsized")) {
            val img = Image.loadFromFile(file.toAbsolutePath.toString)
            val newImg = Image.withDimensions(32, 32)

            if (img.width > 32) {
              for (x <- 0 until 96 by 3) {
                for (y <- 0 until 96 by 3) {
                  newImg(x / 3, y / 3) = img(x, y)
                }
              }

              val relativePath = root.relativize(file)
              Files.createDirectories(output.resolve(relativePath).getParent)
              Image.save(newImg, output.resolve(relativePath).toFile)
            }
          } else {
            println("Skipping : " + file.toString)
          }
          FileVisitResult.CONTINUE
        }
      })
    }
  }
}
