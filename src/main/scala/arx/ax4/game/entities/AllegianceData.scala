package arx.ax4.game.entities

import arx.core.macros.GenerateCompanion
import arx.engine.entity.Entity
import arx.graphics.helpers.Color

@GenerateCompanion
class AllegianceData extends AxAuxData {
	var faction : Entity = Entity.Sentinel
}


@GenerateCompanion
class FactionData extends AxAuxData {
	var color : Color = Color.White
	var playerControlled : Boolean = true
}