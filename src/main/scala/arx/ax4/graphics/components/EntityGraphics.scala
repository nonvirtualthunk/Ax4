package arx.ax4.graphics.components
import arx.ax4.game.entities.{CharacterInfo, Physical, Tile, Tiles}
import arx.ax4.graphics.data.{AxDrawingConstants, CullingData}
import arx.ax4.graphics.resources.CharacterImageset
import arx.core.units.UnitOfTime
import arx.core.vec.Vec2f
import arx.core.vec.coordinates.{AxialVec, CartVec, Hex}
import arx.engine.graphics.components.DrawPriority
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.helpers.{Color, RGBA}
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
				val rawCenter = tilePos.asCartesian(const.HexSize) + game.data[Physical](ent).offset
				for (layer <- imageset.drawInfoFor(game, ent, display.view)) {
					val color = game.dataOpt[Physical](ent).map(p => p.colorTransforms.foldRight(Color.White)((transform, cur) => transform.apply(cur))).getOrElse(Color.White)

					canvas.quad(rawCenter)
   					.hexBottomOrigin(-0.05f, 0.2f)
   					.texture(layer.image)
   					.color(color)
   					.dimensions(layer.image.width * 4, layer.image.height * 4)
   					.layer(DrawLayer.Entity)
   					.draw()
				}


				val frame = ResourceManager.image("graphics/ui/vertical_bar_frame.png")
				val content = ResourceManager.image("graphics/ui/vertical_bar_content.png")

				val stamFrame = ResourceManager.image("graphics/ui/stamina_frame.png")
				val stamCont = ResourceManager.image("graphics/ui/stamina_content.png")

				val healthBarStart = rawCenter + Vec2f((0.25f * const.HexSize).toInt, 0.0f)
				val base = canvas.quad(healthBarStart)
					.color(Color.White)
					.hexBottomOrigin(0.0f, 0.2f)
					.layer(DrawLayer.Entity)

				base.copy()
   				.texture(frame, 4)
   				.draw()

				val fullHeight = content.height * 4
				val fullWidth = content.width * 4
				val practicalWidth = 3 * 4
				base.copy()
					.position(healthBarStart + Vec2f(0,4))
   				.color(RGBA(0.8f,0.1f,0.1f,1.0f))
   				.texture(content)
   				.dimensions(fullWidth, fullHeight * (characterInfo.health.currentValue.toFloat / characterInfo.health.maxValue.toFloat))
   				.draw()

				for (i <- 0 until characterInfo.stamina.maxValue) {
					val staminaStart = healthBarStart + Vec2f(practicalWidth, i * 3 * 4)
					base.copy()
						.position(staminaStart)
						.texture(stamFrame, 4)
						.draw()

					if (i < characterInfo.stamina.currentValue) {
						base.copy()
							.position(staminaStart)
							.texture(stamCont, 4)
							.color(RGBA(0.1f, 0.7f, 0.2f, 1.0f))
							.draw()
					}
				}
			}

		}

	}
}

object EntityGraphics {
	val characterImageset = CharacterImageset.load(ResourceManager.sml("graphics/data/entity/Characters.sml"))
}