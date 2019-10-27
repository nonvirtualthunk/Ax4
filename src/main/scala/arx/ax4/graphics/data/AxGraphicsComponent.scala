package arx.ax4.graphics.data

import arx.core.math.Recti
import arx.core.units.UnitOfTime
import arx.core.vec.coordinates.AxialVec
import arx.core.vec.{ReadVec2f, Vec2f, Vec3f}
import arx.engine.graphics.components.GraphicsComponent
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.pov.TCamera

abstract class AxGraphicsComponent extends GraphicsComponent {
	override final def draw(game: World, display: World): Unit = {
		val animWD = display.worldData[AxAnimatingWorldData]
		draw(animWD.animatedGameWorldView, display)
	}

	def draw(game : HypotheticalWorldView, display: World): Unit


	override protected def onUpdate(game: World, display: World, dt: UnitOfTime, time: UnitOfTime): Unit = {
		val animWD = display.worldData[AxAnimatingWorldData]
		onUpdate(animWD.animatedGameWorldView, display, dt)
	}

	def onUpdate(game : HypotheticalWorldView, display : World, dt : UnitOfTime) : Unit

	def pixelToHex(pov : TCamera, pixel : ReadVec2f, viewport : Recti, hexSize : Int) = {
		val unprojected = pov.unproject(Vec3f(pixel,0.0f), viewport)
		AxialVec.fromCartesian(unprojected.xy, hexSize)
	}
	def hexToPixel(pov : TCamera, hex : AxialVec, viewport : Recti, hexSize : Int) = {
		val px = hex.cartesianX(hexSize)
		val py = hex.cartesianY(hexSize)

		Vec2f(px + pov.eye.x + (viewport.width >> 1), py + pov.eye.y + (viewport.height >> 1))
	}
}


