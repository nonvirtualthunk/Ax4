package arx.ax4.game.event

import arx.ax4.game.entities.LockedCard
import arx.engine.entity.Entity
import arx.engine.event.GameEvent

object CardEvents {
	case class CardDrawn(entity : Entity, card : Entity) extends GameEvent

	case class HandDrawn(entity : Entity) extends GameEvent

	case class CardDiscarded(entity : Entity, card : Entity, explicitDiscard : Boolean) extends GameEvent

	case class HandDiscarded(entity : Entity, cards : Seq[Entity]) extends GameEvent

	case class DeckShuffled(entity : Entity) extends GameEvent

	case class DrawPileShuffled(entity : Entity) extends GameEvent

	case class DiscardPileShuffled(entity : Entity) extends GameEvent

	case class CardPlayed(entity : Entity, card : Entity) extends GameEvent

	case class CardRemoved(entity : Entity, card : Entity) extends GameEvent

	case class CardAdded(entity : Entity, card : Entity) extends GameEvent

	case class LockedCardChanged(entity : Entity, index : Int, newLockedCard : LockedCard) extends GameEvent
}
