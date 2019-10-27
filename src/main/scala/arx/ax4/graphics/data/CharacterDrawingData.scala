package arx.ax4.graphics.data

import arx.ax4.game.entities.Physical
import arx.core.macros.GenerateCompanion
import arx.core.vec.coordinates.CartVec3
import arx.engine.graphics.data.TGraphicsData
import arx.engine.world.WorldView
import arx.graphics.helpers.HSBA

@GenerateCompanion
class CharacterDrawingData extends AxGraphicsData {
	var exactPositionOverride : Option[CartVec3] = None
	var colorMultiplier : HSBA = HSBA.White

	def exactPosition(view : WorldView, phys : Physical) = exactPositionOverride.getOrElse(phys.position.asCartesian)
}
