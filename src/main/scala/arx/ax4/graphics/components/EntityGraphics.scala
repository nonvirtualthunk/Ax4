package arx.ax4.graphics.components
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.{CharacterInfo, FlagLibrary, Physical, TagData, Tile, Tiles}
import arx.ax4.game.logic.TagLogic
import arx.ax4.graphics.data.{AxDrawingConstants, CullingData, TacticalUIData}
import arx.ax4.graphics.resources.CharacterImageset
import arx.core.datastructures.Watcher
import arx.core.units.UnitOfTime
import arx.core.vec.{Vec2f, Vec2i}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, CartVec, Hex}
import arx.engine.graphics.components.DrawPriority
import arx.engine.simple.{DrawLayer, HexCanvas}
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.data.SpriteLibrary
import arx.graphics.helpers.{Color, RGBA}
import arx.resource.ResourceManager

class EntityGraphics extends AxCanvasGraphicsComponent {

	var iconOpacity = Map[AxialVec,Float]().withDefaultValue(0.0f)
	var cullRevisionWatcher : Watcher[Long] = Watcher(0L)

	override def drawPriority = DrawPriority.Late
	override protected def onInitialize(game: World, display: World): Unit = {
		val cullData = display[CullingData]
		cullRevisionWatcher = Watcher(cullData.revision)
	}

	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = {
//		game.areAnyHypotheticallyModified(CharacterInfo, Physical) || cullRevisionWatcher.hasChanged
		true
	}

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: HexCanvas, dt: UnitOfTime): Unit = {
		implicit val view = game

		val imageset = EntityGraphics.characterImageset
		val cull = display[CullingData]
		val const = display[AxDrawingConstants]


		val baseSpriteHeight = 24
		val targetSpriteHeight = (const.HexHeight * 0.8f).toInt
		val scale = targetSpriteHeight / baseSpriteHeight
		val spriteHeight = baseSpriteHeight * scale

		for (tilePos <- cull.hexesByCartesianCoord;
			  tileEnt = Tiles.tileAt(tilePos.q, tilePos.r);
			  tile <- game.dataOpt[Tile](tileEnt);
			  ent <- tile.entities) {

			for (characterInfo <- game.dataOpt[CharacterInfo](ent)) {
				val rawCenter = tilePos.asCartesian + game.data[Physical](ent).offset
				for (layer <- imageset.drawInfoFor(game, ent, display.view)) {
					val color = game.dataOpt[Physical](ent).map(p => p.colorTransforms.foldRight(Color.White)((transform, cur) => transform.apply(cur))).getOrElse(Color.White)

					canvas.quad(rawCenter)
   					.hexBottomOrigin(-0.05f, 0.2f)
   					.texture(layer.image, scale)
   					.color(color)
   					.layer(DrawLayer.Entity)
   					.draw()
				}

				var opacity = iconOpacity(tilePos)
				if (display[TacticalUIData].mousedOverHex == AxialVec3(tilePos, 0)) {
					opacity = (opacity + 0.1f).min(1.0f)
				} else {
					opacity = (opacity - 0.1f).max(0.0f)
				}
				iconOpacity += tilePos -> opacity

				if (opacity > 0.01f) {
					val scale = 2
					var offset = 0
					for ((flag, count) <- TagLogic.allFlags(ent) if count != 0;
							 flagInfo <- FlagLibrary.getWithKind(flag) if !flagInfo.hidden;
							 spriteInfo <- SpriteLibrary.getSpriteDefinitionFor(flag)) {

						val pos = Util.posWithinHex(rawCenter, Vec2f(-0.25f,-0.4f), const) + Vec2f(offset, 0)

						canvas.quad(pos)
							.texture(spriteInfo.icon)
							.dimensions(32 * scale, 32 * scale)
							.color(RGBA(1.0f,1.0f,1.0f,opacity))
							.layer(DrawLayer.Entity)
							.draw()

						Drawing.drawNumber(canvas, count, pos + Vec2f(16*scale-10,-16*scale+10), 1, RGBA(1.0f,1.0f,1.0f,opacity))

						offset += 32*scale + 10
					}
				}


				val frame = ResourceManager.image("graphics/ui/vertical_bar_frame.png")
				val content = ResourceManager.image("graphics/ui/vertical_bar_content.png")

				val stamFrame = ResourceManager.image("graphics/ui/stamina_frame.png")
				val stamCont = ResourceManager.image("graphics/ui/stamina_content.png")



				val healthBarStart = (rawCenter + CartVec(0.25f, -0.3f)).asPixels(const.HexSize)
				val base = canvas.quad(healthBarStart)
					.color(Color.White)
					.centered(false)
					.layer(DrawLayer.Entity)

				base.copy()
   				.texture(frame, scale)
   				.draw()

				val fullHeight = content.height * scale
				val fullWidth = content.width * scale
				val practicalWidth = 3 * scale
				base.copy()
					.position(healthBarStart)
  				.offset(Vec2f(scale,scale))
   				.color(RGBA(0.8f,0.1f,0.1f,1.0f))
   				.texture(content)
   				.dimensions(fullWidth, fullHeight * (characterInfo.health.currentValue.toFloat / characterInfo.health.maxValue.toFloat))
   				.draw()

				Drawing.drawNumber(canvas, characterInfo.health.currentValue, healthBarStart + Vec2f(10, fullHeight + 32), 2, RGBA(0.9f,0.2f,0.1f,opacity))

				for (i <- 0 until characterInfo.stamina.maxValue) {
					val staminaStart = healthBarStart + Vec2f(practicalWidth, i * 3 * scale)
					base.copy()
						.position(staminaStart)
						.texture(stamFrame, scale)
						.draw()

					if (i < characterInfo.stamina.currentValue) {
						base.copy()
							.position(staminaStart)
							.texture(stamCont, scale)
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