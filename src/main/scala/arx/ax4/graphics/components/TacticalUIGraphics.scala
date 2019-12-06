package arx.ax4.graphics.components
import arx.Prelude
import arx.ax4.control.components.ActionSelectionContext
import arx.ax4.game.action.GameAction
import arx.ax4.game.entities.Companions.{AllegianceData, CharacterInfo, FactionData, Physical}
import arx.ax4.game.entities.{CharacterInfo, Physical}
import arx.ax4.graphics.components.subcomponents.{TacticalActionPreviewRenderer, TacticalSelectorRenderer}
import arx.ax4.graphics.data.{AxDrawingConstants, TacticalUIData}
import arx.ax4.graphics.logic.EntityDrawLogic
import arx.core.introspection.ReflectionAssistant
import arx.core.units.UnitOfTime
import arx.core.vec.{Vec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, CartVec, CartVec3}
import arx.engine.control.data.TControlData
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.helpers.{Color, RGBA}
import arx.resource.ResourceManager

class TacticalUIGraphics(anim : AnimationGraphicsComponent) extends AxCanvasGraphicsComponent {

	lazy val actionRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalActionPreviewRenderer]
	lazy val selectorRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalSelectorRenderer]
	lazy val intentOverlays = ReflectionAssistant.instancesOfSubtypesOf[GameActionIntentOverlay]

	override protected def onInitialize(game: World, display: World): Unit = {

	}

	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = true




	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: AxCanvas, dt: UnitOfTime): Unit = {
		implicit  val view = game
		val tuid = display[TacticalUIData]
		import tuid._
		val const = display[AxDrawingConstants]

		val selImg = ResourceManager.image("graphics/ui/selection_arrow.png")
		val selScale = 4//(const.HexSize / selImg.width)

		val dy = (math.cos(Prelude.curTime().inSeconds * 2.0f) * 0.03f).toFloat
		for (selC <- selectedCharacter) {
			val pos = EntityDrawLogic.characterPosition(selC).getOrElse(CartVec3.Zero)

			canvas.quad(pos.xy + CartVec(0.0f,0.5f + dy))
				.layer(DrawLayer.OverEntity)
				.texture(selImg)
				.dimensions(selImg.width * selScale, selImg.height * selScale)
				.color(Color.White)
				.draw()

			val ap = selC(CharacterInfo).actionPoints
			val fractionalIndicator = if (ap.currentValue == ap.maxValue) {
				"graphics/ui/selection_arrow.png"
			} else {
				s"graphics/ui/selection_arrow_${ap.currentValue}_${ap.maxValue}.png"
			}
			if (ap.currentValue > 0) {
				canvas.quad(pos.xy + CartVec(0.0f, 0.5f + dy))
					.layer(DrawLayer.OverEntity)
					.texture(fractionalIndicator)
					.dimensions(selImg.width * selScale, selImg.height * selScale)
					.color(selC(AllegianceData).faction(FactionData).color)
					.draw()
			}


			consideringActionSelectionContext match {
				case Some(ActionSelectionContext(intent, selectionResults)) =>
					if (!intent.hasRemainingSelections(selectionResults)) {
						for (action <- intent.createAction(selectionResults.build()) ; renderer <- actionRenderers) {
							renderer.previewAction(game, display, canvas).lift.apply(action)
						}
					} else {
						for ((selector, values) <- selectionResults.results ; selRenderer <- selectorRenderers) {
							selRenderer.renderSelection(game, display, canvas, selector, values)
						}
					}
				case None => // do nothing
			}

			intentOverlays.foreach(overlay => overlay.
				draw(view, display, selC[CharacterInfo].activeIntent, consideringActionSelectionContext, canvas))
		}

//		canvas.quad(mousedOverHex)
//   		.layer(DrawLayer.OverEntity)
//   		.texture("third-party/DHGZ/targetIcon.png", 3)
//   		.draw()
//
//		canvas.quad(mousedOverHex.asCartesian(const.HexSize.toFloat).xy + AxialVec.CartesianDelta(mousedOverHexBiasDir) * const.HexSize * 0.25f)
//   		.layer(DrawLayer.OverEntity)
//   		.texture(s"third-party/DHGZ/hexArrow${mousedOverHexBiasDir}.png", 2)
//   		.draw()
	}
}