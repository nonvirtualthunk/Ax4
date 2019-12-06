package arx.ax4.graphics.logic

import arx.ax4.graphics.data.AxDrawingConstants
import arx.core.math.Recti
import arx.core.vec.{Vec2f, Vec3f}
import arx.core.vec.coordinates.AxialVec3
import arx.engine.graphics.data.{PovData, WindowingGraphicsData}
import arx.engine.world.World
import arx.graphics.GL

object GameWidgetLogic {

	def gamePositionToWidgetPosition(v : AxialVec3)(display : World) = {
		val const = display[AxDrawingConstants]
		val cart = v.asCartesian(const.HexSizef) - Vec3f(0.125f * const.HexSizef,-0.125f * const.HexSizef,0.0f)

		val camera = display[PovData].pov

		val projected = camera.project(cart, GL.viewport)

		val desktop = display[WindowingGraphicsData].desktop
		val xy = projected.xy
		Vec2f(desktop.drawing.effectiveDimensions.x * 0.5f + xy.x, desktop.drawing.effectiveDimensions.y * 0.5f - xy.y)
	}

}
