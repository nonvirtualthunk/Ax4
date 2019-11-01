package arx.ax4.game.event

import arx.engine.entity.Entity
import arx.engine.event.GameEvent

object TurnEvents {
	case class TurnStartedEvent(faction : Entity, turnNumber : Int) extends GameEvent

	case class TurnEndedEvent(faction : Entity, turnNumber : Int) extends GameEvent
}
