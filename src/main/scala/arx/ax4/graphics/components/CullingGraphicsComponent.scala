package arx.ax4.graphics.components

import arx.ax4.graphics.data.{AxDrawingConstants, AxGraphicsComponent, CullingData}
import arx.core.datastructures.Watcher
import arx.core.math.Recti
import arx.core.units.UnitOfTime
import arx.core.vec.Vec2f
import arx.core.vec.coordinates.{AxialVec, HexRingIterator}
import arx.engine.EngineCore
import arx.engine.graphics.data.PovData
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.GL

class CullingGraphicsComponent extends AxGraphicsComponent {
	val margin = -300

	var lastCameraPos = AxialVec(-100000,-10000)
	var cameraPosWatcher : Watcher[AxialVec] = _
	var viewportWatcher : Watcher[Recti] = _
	var first = true

	override protected def onInitialize(game: World, display: World): Unit = {
		val povData = display.worldData[PovData]
		val pov = povData.pov
		val const = display.worldData[AxDrawingConstants]
		cameraPosWatcher = Watcher(AxialVec.fromCartesian(pov.eye.xy, const.HexSize))
		viewportWatcher = Watcher(display.worldData[CullingData].mainDrawAreaFunc(GL.viewport))
	}


	override def onUpdate(game: HypotheticalWorldView, display: World, dt: UnitOfTime): Unit = {
		val pov = display.worldData[PovData].pov
		val const = display.worldData[AxDrawingConstants]

		if (first || cameraPosWatcher.hasChanged || viewportWatcher.hasChanged) {
			first = false
			val cullData = display.worldData[CullingData]

			val viewport = cullData.mainDrawAreaFunc(GL.viewport)

			val centerHex = pixelToHex(pov, Vec2f(viewport.x + viewport.width / 2, viewport.y + viewport.height / 2) / EngineCore.pixelScaleFactor, viewport, const.HexSize)

			var inViewHexes = Set[AxialVec]()
			var radius = 0
			while (radius < 30) {
				val iter = HexRingIterator(centerHex, radius)
				while (iter.hasNext) {
					val hex = iter.next()
					val pixel = hexToPixel(pov, hex, viewport, const.HexSize)

					if (pixel.x > viewport.x + margin && pixel.x < viewport.maxX - margin && pixel.y > viewport.minY + margin && pixel.y < viewport.maxY - margin) {
						inViewHexes += hex
					}
				}
				radius += 1
			}

			cullData.hexesInView = inViewHexes
			cullData.hexesByCartesianCoord = inViewHexes.toList.sortBy(v => -v.cartesianY() + v.cartesianX() * 0.0001f)
			cullData.cameraCenter = cameraPosWatcher.peek()
			cullData.currentViewport = viewportWatcher.peek()
			cullData.revision += 1
		}
	}

	override def draw(game: HypotheticalWorldView, graphics: World): Unit = {}

}

