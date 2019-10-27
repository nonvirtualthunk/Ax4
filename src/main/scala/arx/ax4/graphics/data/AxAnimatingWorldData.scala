package arx.ax4.graphics.data

import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.{HypotheticalWorld, HypotheticalWorldView, World, WorldView}

class AxAnimatingWorldData extends AxGraphicsData with TMutableAuxData with TWorldAuxData {
	// just so we don't have to have things uninitialized
	protected var placeholderWorld = new World
	var currentGameWorldView : WorldView = placeholderWorld.view
	var hypotheticalWorld : HypotheticalWorld = new HypotheticalWorld(placeholderWorld, currentGameWorldView)
	def animatedGameWorldView : HypotheticalWorldView = hypotheticalWorld.view
}
