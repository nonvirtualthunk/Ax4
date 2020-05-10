package arx.ax4.graphics.components

import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{FileVisitResult, Files, Path, Paths, SimpleFileVisitor}

import arx.Prelude
import arx.Prelude.permute
import arx.ax4.game.entities.Companions.{Terrain, Tile, Vegetation}
import arx.ax4.game.entities.{Terrain, Tile, Tiles, Vegetation}
import arx.ax4.graphics.data.{AxDrawingConstants, AxGraphicsComponent, AxGraphicsData, CullingData}
import arx.ax4.graphics.resources.{ImageLayer, Tileset}
import arx.core.datastructures.Watcher
import arx.core.mathutil.RunningVector
import arx.core.units.UnitOfTime
import arx.core.vec.coordinates.{AxialVec, CartVec, Hex}
import arx.core.vec.{ReadVec2f, Vec2f, Vec2i, Vec3f, Vec4f, Vec4i}
import arx.engine.data.TWorldAuxData
import arx.engine.entity.Taxon
import arx.engine.graphics.components.DrawPriority
import arx.engine.graphics.data.PovData
import arx.engine.simple.HexCanvas
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.helpers.{Color, HSBA}
import arx.graphics.{AVBO, GL, Image, TextureBlock}
import arx.resource.ResourceManager
import org.lwjgl.opengl.{GL11, GL15}

import scala.collection.mutable

class TileGraphics(cullingComponent : CullingGraphicsComponent) extends AxCanvasGraphicsComponent {
	var cullRevisionWatcher : Watcher[Long] = _
	var tileset : Tileset = _

	override def drawPriority = DrawPriority.Early

	override protected def onInitialize(game: World, display: World): Unit = {
		val cullData = display[CullingData]
		cullRevisionWatcher = Watcher(cullData.revision)
		tileset = Tileset.load(ResourceManager.sml("graphics/data/tilesets/Tileset.sml"))
	}


	override def requiresUpdate(game: HypotheticalWorldView, display: World): Boolean = {
		game.areAnyHypotheticallyModified(Tile, Vegetation, Terrain) || cullRevisionWatcher.hasChanged
	}

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: HexCanvas, dt: UnitOfTime): Unit = {
		val const = display[AxDrawingConstants]
		val cullData = display[CullingData]

		for (tilePos <- cullData.hexesByCartesianCoord;
			  tileEnt = Tiles.tileAt(tilePos.q, tilePos.r);
			  tile <- game.dataOpt[Tile](tileEnt)) {

			for (ImageLayer(img, color) <- tileset.drawInfoFor(tilePos, game.data[Terrain](tileEnt), game.data[Vegetation](tileEnt))) {

				val roundedWidth = (const.HexSize / img.width) * img.width
				val roundedHeight = (const.HexSize / img.height) * img.height

				canvas.quad(tilePos)
					.hexBottomOrigin()
					.dimensions(Vec2f(roundedWidth, roundedHeight))
					.color(color)
					.lightColor(Color.White)
					.texture(img)
					.visionPcnt(1.0f)
					.draw()
			}
		}
	}


}

object TileGraphics {
	class Data extends AxGraphicsData with TWorldAuxData {
		val imagesForTerrain = new mutable.HashMap[Taxon, Image]()
	}
}
