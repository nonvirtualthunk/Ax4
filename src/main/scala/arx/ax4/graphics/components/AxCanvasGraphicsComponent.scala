package arx.ax4.graphics.components

import java.util.concurrent.atomic.{AtomicBoolean, AtomicLong}

import arx.ax4.graphics.data.{AxDrawingConstants, AxGraphicsComponent}
import arx.core.units.UnitOfTime
import arx.engine.graphics.data.PovData
import arx.engine.simple.HexCanvas
import arx.engine.world.{HypotheticalWorldView, World}
import arx.graphics.GL
import arx.graphics.shader.Shader
import arx.resource.ResourceManager
import org.lwjgl.opengl.GL11

abstract class AxCanvasGraphicsComponent extends AxGraphicsComponent {

	val canvas : HexCanvas = new HexCanvas(128)
	canvas.useHighCapacity(true)
	canvas.useTexFilters(GL11.GL_LINEAR, GL11.GL_NEAREST)
	var updatePending = new AtomicBoolean(false)
	var lastDrawnWorldTimeMarker = new AtomicLong(0L)

	def shader : Shader = ResourceManager.shader("shaders/ax4/AxMain")
	def depthFunc : Int = GL11.GL_LEQUAL
	def depthTestEnabled : Boolean = true

	override def draw(game: HypotheticalWorldView, display: World): Unit = {
		shader.bind()
		GL.glSetState(GL11.GL_DEPTH_TEST, enable = depthTestEnabled)
		GL.glSetDepthFunc(depthFunc)

		display[PovData].pov.look()

		canvas.draw()
	}

	override final def onUpdate(game: HypotheticalWorldView, display: World, dt: UnitOfTime): Unit = {
		canvas.hexSize = display[AxDrawingConstants].HexSize
		val drawMarker = game.root.currentTime
		if (!updatePending.get() && (drawMarker.time != lastDrawnWorldTimeMarker.get() || requiresUpdate(game, display))) {
			updatePending.set(true)
			// we can set this even though we haven't drawn yet, since updatePending will ensure
			// that a draw is done at some point
			lastDrawnWorldTimeMarker.set(drawMarker.time)
		}

		if (updatePending.get()) {
			if (canvas.startUpdate()) {

				updateCanvas(game, display, canvas, dt)

				canvas.finishUpdate()
				updatePending.set(false)
			}
		}
	}

	def requiresUpdate(game : HypotheticalWorldView, display : World) : Boolean
	def updateCanvas(game: HypotheticalWorldView, display: World, canvas : HexCanvas, dt: UnitOfTime): Unit
}
