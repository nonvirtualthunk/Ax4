package arx.ax4.game.entities

import arx.core.macros.GenerateCompanion
import arx.engine.data.TWorldAuxData
import arx.engine.entity.Entity

@GenerateCompanion
class TurnData extends TWorldAuxData {
	var turn : Int = 0
	var activeFaction : Entity = Entity.Sentinel
}
