package arx.ax4.graphics.components
import arx.ax4.game.entities.{CharacterInfo, Physical, Tile, Tiles}
import arx.ax4.graphics.data.{AxDrawingConstants, CullingData}
import arx.ax4.graphics.resources.CharacterImageset
import arx.core.units.UnitOfTime
import arx.core.vec.Vec2f
import arx.core.vec.coordinates.{AxialVec, Hex}
import arx.engine.graphics.components.DrawPriority
import arx.engine.world.{HypotheticalWorldView, World}
import arx.resource.ResourceManager

class EntityGraphics extends AxCanvasGraphicsComponent {

	override def drawPriority = DrawPriority.Late
	override protected def onInitialize(game: World, display: World): Unit = {}

	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = {
		true
	}

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: AxCanvas, dt: UnitOfTime): Unit = {
//		val const = display[AxDrawingConstants]
//		val img = ResourceManager.image("third-party/test_character.png")
//		val scale = (Hex.heightForSize(const.HexSize) * 0.85).toInt / img.height
//		canvas.quad(Vec2f(0.0f, 16))
//		.hexBottomOrigin()
//   		.texture(img)
//   		.dimensions(img.width * 4, img.height * 4)
//   		.draw()
//
//
//
		val imageset = EntityGraphics.characterImageset
		val cull = display[CullingData]
		val const = display[AxDrawingConstants]
		for (tilePos <- cull.hexesByCartesianCoord;
			  tileEnt = Tiles.tileAt(tilePos.q, tilePos.r);
			  tile <- game.dataOpt[Tile](tileEnt);
			  ent <- tile.entities) {

			for (characterInfo <- game.dataOpt[CharacterInfo](ent)) {
				for (layer <- imageset.drawInfoFor(game, ent, display.view)) {
					canvas.quad(tilePos.asCartesian(const.HexSize) + game.data[Physical](ent).offset)
   					.hexBottomOrigin(0.5f)
   					.texture(layer.image)
   					.dimensions(layer.image.width * 4, layer.image.height * 4)
   					.layer(DrawLayer.Entity)
   					.draw()
				}
			}

		}

	}
}

object EntityGraphics {
	val characterImageset = CharacterImageset.load(ResourceManager.sml("graphics/data/entity/Characters.sml"))
}