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

	override def updateCanvas(game: HypotheticalWorldView, display: World, canvas: AxCanvas, dt: UnitOfTime): Unit = {
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

object HexMaskGenerator {
	def main(args: Array[String]): Unit = {
		val img = Image.withDimensions(125, 109)

		img.setPixelsFromFunc((x,y) => {
			if (AxialVec.fromCartesian(Vec2f(x - img.width / 2, y - img.height / 2), (img.width-1).toFloat) == AxialVec(0,0)) {
				Vec4i(255, 255, 255, 255)
			} else {
				Vec4i(0, 0, 0, 255)
			}
		})

		Image.save(img, "/tmp/hex.png")
	}

	def maskImageToHex(img : Image) = {
		val runningVec = new RunningVector
		img.transformPixelsByFunc((x,y, c) => {
			if (AxialVec.fromCartesian(Vec2f(x - img.width / 2, y - img.height / 2), (img.width-1).toFloat) == AxialVec(0,0)) {
				runningVec.value(x, y)
				c
			} else {
				Vec4i(c.r, c.g, c.b, 0)
			}
		})
		img.cropped(Vec2i(runningVec.min.xy), Vec2i(runningVec.max.xy))
		img
	}
}

object Downsizer {
	def main(args: Array[String]): Unit = {
		val output = Paths.get("/Users/nvt/Code/Ax4/src/main/resources/third-party/zeshioModified")
		val root = Paths.get("/Users/nvt/Code/Ax4/src/main/resources/third-party/ZeshiosPixelHexTileset1.1_HexKit/")
		Files.walkFileTree(root, new SimpleFileVisitor[Path] {
			override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
				if (file.toString.endsWith("png") && ! file.toString.contains("upsized")) {
					val img = Image.loadFromFile(file.toAbsolutePath.toString)
					val newImg = Image.withDimensions(32, 32)

					if (img.width > 32) {
						for (x <- 0 until 96 by 3) {
							for (y <- 0 until 96 by 3) {
								newImg(x / 3, y / 3) = img(x, y)
							}
						}

						val relativePath = root.relativize(file)
						Files.createDirectories(output.resolve(relativePath).getParent)
						Image.save(newImg, output.resolve(relativePath).toFile)
					}
				} else {
					println("Skipping : " + file.toString)
				}
				FileVisitResult.CONTINUE
			}
		})
	}
}