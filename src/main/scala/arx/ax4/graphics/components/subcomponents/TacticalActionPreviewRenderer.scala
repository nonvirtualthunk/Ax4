package arx.ax4.graphics.components.subcomponents

import arx.ai.search.PathStep
import arx.application.Noto
import arx.ax4.game.action.{AttackAction, BiasedAxialVec3, BiasedHexSelector, GameAction, HexSelector, MoveAction, Selector}
import arx.ax4.game.entities.Companions.Physical
import arx.ax4.graphics.components.{AxCanvas, DrawLayer}
import arx.core.vec.Vec3f
import arx.core.vec.coordinates.AxialVec3
import arx.engine.world.{HypotheticalWorldView, World}
import arx.resource.ResourceManager
import arx.Prelude._
import arx.graphics.helpers.RGBA

trait TacticalActionPreviewRenderer {
	def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _]
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
			} else {
				Noto.warn(s"trying to preview degenerate path : $path")
			}
	}
}

class AttackActionPreviewRenderer extends TacticalActionPreviewRenderer {
	override def previewAction(game: HypotheticalWorldView, display: World, canvas: AxCanvas): PartialFunction[GameAction, _] = {
		case AttackAction(attacker, attack, attackFrom, targets, preMove, postMove) =>
			implicit val view = game
			val img = ResourceManager.image("third-party/DHGZ/sword1.png")
			val startPos = attackFrom
			val endPos = targets match {
				case Left(entities) => entities.head(Physical).position
				case Right(hexes) => hexes.head
			}

			canvas.quad((startPos.qr.asCartesian + endPos.qr.asCartesian) * 0.5f)
				.texture(img, 4)
				.layer(DrawLayer.OverEntity)
				.draw()

			targets match {
				case Left(entities) =>
					for (e <- entities) {
						canvas.quad(e(Physical).position)
   						.texture(ResourceManager.image("third-party/DHGZ/targetIcon.png"),4)
   						.layer(DrawLayer.OverEntity)
   						.draw()
					}
				case Right(hexes) =>
					for (h <- hexes) {
						canvas.quad(h)
   						.texture(ResourceManager.image("third-party/zeshioModified/ui/hex_selection.png"))
   						.color(RGBA(0.7f,0.1f,0.1f,0.8f))
   						.hexBottomOrigin()
   						.layer(DrawLayer.UnderEntity)
   						.draw()
					}
			}
	}
}


trait TacticalSelectorRenderer {
	def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any])
}

class HexSelectorRenderer extends TacticalSelectorRenderer {
	override def renderSelection(game: HypotheticalWorldView, display: World, canvas: AxCanvas, selector: Selector[_], selected: List[Any]) = selector pmatch {
		case HexSelector(pattern, hexPredicate) =>
			selected.foreach {
				case a: AxialVec3 => canvas.quad(a)
					.layer(DrawLayer.UnderEntity)
					.texture("third-party/DHGZ/frame11.png")
					.draw()
				case other => Noto.warn(s"Unexpected selected value for hex selector $other")
			}
		case BiasedHexSelector(pattern, hexPredicate) =>
			selected.foreach {
				case a: BiasedAxialVec3 => canvas.quad(a.vec)
					.layer(DrawLayer.UnderEntity)
					.texture("third-party/DHGZ/frame11.png")
					.draw()
				case other => Noto.warn(s"Unexpected selected value for hex selector $other")
			}
	}
}