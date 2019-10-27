package arx.ax4.graphics.components
import arx.Prelude
import arx.ax4.game.entities.Companions.{AllegianceData, FactionData, Physical}
import arx.ax4.game.entities.{CharacterInfo, Physical}
import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.units.UnitOfTime
import arx.core.vec.Vec2f
import arx.engine.control.data.TControlData
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.entity.Entity
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.helpers.RGBA
import arx.resource.ResourceManager

class TacticalUIGraphics extends AxCanvasGraphicsComponent {
	override protected def onInitialize(game: World, display: World): Unit = {

	}

	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = true

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: AxCanvas, dt: UnitOfTime): Unit = {
		implicit  val view = game
		val UID = display[TacticalUIData]
		import UID._
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
		}
	}
}

class TacticalUIData extends TControlData with TMutableAuxData with TWorldAuxData {
	var selectedCharacter : Option[Entity] = None
}