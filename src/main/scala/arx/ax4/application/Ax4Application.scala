package arx.ax4.application

import arx.ax4.application.scenarios.SimpleMapScenario
import arx.engine.Engine
import arx.engine.world.Universe

object Ax4Application extends Engine {

	override def onInit() {
		loadScenario(SimpleMapScenario, new Universe)
	}


}
