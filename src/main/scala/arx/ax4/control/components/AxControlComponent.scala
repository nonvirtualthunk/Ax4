package arx.ax4.control.components

import arx.ax4.graphics.data.AxAnimatingWorldData
import arx.core.units.UnitOfTime
import arx.engine.control.components.ControlComponent
import arx.engine.world.{HypotheticalWorldView, World}

abstract class AxControlComponent extends ControlComponent {
	override protected def onUpdate(game: World, display: World, dt: UnitOfTime): Unit = {
		val animWD = display.worldData[AxAnimatingWorldData]
		onUpdate(animWD.animatedGameWorldView, game, display, dt)
	}

	protected def onUpdate(gameView : HypotheticalWorldView, game : World, display : World, dt : UnitOfTime) : Unit

	override protected def onInitialize(game: World, display: World): Unit = {
		val animWD = display.worldData[AxAnimatingWorldData]
		onInitialize(animWD.animatedGameWorldView, game, display)
	}

	protected def onInitialize(view: HypotheticalWorldView, game: World, display: World) : Unit
}
