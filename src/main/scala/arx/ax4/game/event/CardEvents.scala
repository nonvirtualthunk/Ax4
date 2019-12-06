package arx.ax4.game.event

import arx.engine.entity.Entity
import arx.engine.event.GameEvent

object CardEvents {
	case class CardDrawn(entity : Entity, card : Entity) extends GameEvent

	case class CardDiscarded(entity : Entity, card : Entity, explicitDiscard : Boolean) extends GameEvent

	case class DeckShuffled(entity : Entity) extends GameEvent
}
