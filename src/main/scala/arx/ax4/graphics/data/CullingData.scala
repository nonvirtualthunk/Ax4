package arx.ax4.graphics.data

import arx.core.datastructures.Watcher
import arx.core.math.Recti
import arx.core.vec.coordinates.AxialVec
import arx.engine.data.TWorldAuxData
import arx.engine.graphics.data.TGraphicsData
import arx.graphics.GL

class CullingData extends AxGraphicsData with TWorldAuxData {
	var cameraCenter = AxialVec.Zero
	var hexesInView = Set[AxialVec]()
	var hexesByCartesianCoord = List[AxialVec]()
	var mainDrawAreaFunc : Recti => Recti = v => v
	var currentViewport : Recti = GL.viewport

	/**
	 * Monotonically increases every time the culling data is recomputed. Simple way for downstream
	 * components to determine if they need to update as a result of camera movement or viewport changes
	 */
	var revision = 0L

	def createRevisionWatcher = Watcher.atLeastOnce(revision)
}
