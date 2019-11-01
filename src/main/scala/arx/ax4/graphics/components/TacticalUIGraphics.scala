package arx.ax4.graphics.components
import arx.Prelude
import arx.ax4.game.action.GameAction
import arx.ax4.game.entities.Companions.{AllegianceData, FactionData, Physical}
import arx.ax4.game.entities.{CharacterInfo, Physical}
import arx.ax4.graphics.components.subcomponents.TacticalActionPreviewRenderer
import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.introspection.ReflectionAssistant
import arx.core.units.UnitOfTime
import arx.core.vec.{Vec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3}
import arx.engine.control.data.TControlData
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.helpers.RGBA
import arx.resource.ResourceManager

class TacticalUIGraphics(anim : AnimationGraphicsComponent) extends AxCanvasGraphicsComponent {

	lazy val renderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalActionPreviewRenderer]

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
			val pos = selC(Physical).position

			canvas.quad(pos.asCartesian(const.HexSize.toFloat).xy + Vec2f(0.0f,const.HexSize * (0.5f + dy)) + selC(Physical).offset)
   			.layer(DrawLayer.OverEntity)
   			.texture(selImg)
   			.dimensions(selImg.width * selScale, selImg.height * selScale)
   			.color(selC(AllegianceData).faction(FactionData).color)
//   			.relativeOrigin(Vec2f(0.0f,-1.0f))
   			.draw()

			for (consideredAction <- consideringActions; renderer <- renderers) {
				renderer.previewAction(game, display, canvas).lift.apply(consideredAction)
			}
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

class TacticalUIData extends TControlData with TMutableAuxData with TWorldAuxData {
	var selectedCharacter : Option[Entity] = None

	var consideringActions : List[GameAction] = Nil
	var consideringSelection : Option[Entity] = None

	var mousedOverHex = AxialVec3.Zero
	var mousedOverHexBiasDir = 0
}