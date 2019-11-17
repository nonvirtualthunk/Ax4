package arx.ax4.graphics.logic

import arx.ax4.game.entities.Physical
import arx.core.vec.coordinates.CartVec3
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object EntityDrawLogic {


	def characterPosition(entity : Entity)(implicit view : WorldView) = {
		entity.dataOpt[Physical].map(p => p.position.asCartesian + CartVec3(p.offset,0))

//		val rawCenter = tilePos.asCartesian(const.HexSize.toFloat) + game.data[Physical](ent).offset
//		for (layer <- imageset.drawInfoFor(game, ent, display.view)) {
//			val color = game.dataOpt[Physical](ent).map(p => p.colorTransforms.foldRight(Color.White)((transform, cur) => transform.apply(cur))).getOrElse(Color.White)
//
//			canvas.quad(rawCenter)
//				.hexBottomOrigin(-0.05f, 0.2f)
//				.texture(layer.image)
//				.color(color)
//				.dimensions(layer.image.width * 4, layer.image.height * 4)
//				.layer(DrawLayer.Entity)
//				.draw()
//		}

	}

}
