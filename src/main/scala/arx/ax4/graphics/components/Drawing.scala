package arx.ax4.graphics.components

import arx.core.vec.{ReadVec2f, ReadVec2i, Vec2f, Vec2i}
import arx.engine.simple.DrawLayer.Entity
import arx.engine.simple.{DrawLayer, HexCanvas, HexQuadBuilder}
import arx.graphics.helpers.{Color, RGBA}
import arx.resource.ResourceManager

object Drawing {
  def drawNumber(canvas : HexCanvas, number : Int, position : ReadVec2f, scale : Int, color : Color, layer : DrawLayer = Entity): Unit = {
    var offset = 0
    if (color.asRGBA.a > 0.0f) {
      if (number < 0) {
        canvas.quad(position)
          .offset(Vec2f(offset, 0))
          .texture(s"graphics/ui/numerals/negative.png", scale)
          .color(color)
          .layer(layer)
          .draw()
        offset += 10
      }

      val numberStr = number.abs.toString
      for (numeralChar <- numberStr) {
        val img = ResourceManager.image(s"graphics/ui/numerals/${numeralChar}_outlined.png")

        canvas.quad(position)
          .offset(Vec2f(offset, 0))
          .texture(img, scale)
          .color(color)
          .layer(layer)
          .draw()

        offset += img.width * scale
      }
    }
  }
}
