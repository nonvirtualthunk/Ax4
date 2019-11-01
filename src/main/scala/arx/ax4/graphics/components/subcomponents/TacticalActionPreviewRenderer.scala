package arx.ax4.graphics.components.subcomponents

import arx.ai.search.PathStep
import arx.application.Noto
import arx.ax4.game.action.{GameAction, MoveAction}
import arx.ax4.game.entities.Companions.Physical
import arx.ax4.graphics.components.{AxCanvas, DrawLayer}
import arx.core.vec.Vec3f
import arx.engine.world.{HypotheticalWorldView, World}
import arx.resource.ResourceManager

trait TacticalActionPreviewRenderer {
	def previewAction(game : HypotheticalWorldView, display : World, canvas: AxCanvas) : PartialFunction[GameAction, _]
}


class MoveActionPreviewRenderer extends TacticalActionPreviewRenderer {
	override def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _] = {
		case MoveAction(entity, path) =>
			if (path.steps.size > 1) {
				implicit val view = game
				val img = ResourceManager.image("third-party/wesnoth/feet.png")
				val startPos = path.steps.head.node
				var prev = startPos

				for (PathStep(hex, cost) <- path.steps; if hex != startPos) {
					val stepDir = (hex.asCartesian(1.0f) - prev.asCartesian(1.0f)).normalize
					val ortho = stepDir.cross(Vec3f.UnitZ)
					canvas.quad(hex)
						.texture(img)
						.dimensions(img.width * 2, img.height * 2)
						.layer(DrawLayer.UnderEntity)
						.forward(stepDir.xy.normalizeSafe)
						.ortho(ortho.xy.normalizeSafe)
						.draw()
					prev = hex
				}
			} else { Noto.warn(s"trying to preview degenerate path : $path")}
	}
}