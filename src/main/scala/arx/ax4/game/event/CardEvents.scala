package arx.ax4.game.event

import arx.ax4.game.entities.{LockedCard, LockedCardSlot, LockedCardType}
import arx.engine.entity.Entity
import arx.engine.event.GameEvent

trait DeckModificationEvent extends GameEvent

object CardEvents {
	case class CardDrawn(entity : Entity, card : Entity) extends GameEvent with DeckModificationEvent

	case class HandDrawn(entity : Entity) extends GameEvent with DeckModificationEvent

	case class CardDiscarded(entity : Entity, card : Entity, explicitDiscard : Boolean) extends GameEvent with DeckModificationEvent

	case class HandDiscarded(entity : Entity, cards : Seq[Entity]) extends GameEvent with DeckModificationEvent

	case class DeckShuffled(entity : Entity) extends GameEvent with DeckModificationEvent

	case class DrawPileShuffled(entity : Entity) extends GameEvent with DeckModificationEvent

	case class DiscardPileShuffled(entity : Entity) extends GameEvent with DeckModificationEvent

	case class CardPlayed(entity : Entity, card : Entity) extends GameEvent with DeckModificationEvent

	case class CardRemoved(entity : Entity, card : Entity) extends GameEvent with DeckModificationEvent

	case class CardAdded(entity : Entity, card : Entity) extends GameEvent with DeckModificationEvent

	case class LockedCardChanged(entity : Entity, index : Int, newLockedCard : LockedCardType) extends GameEvent with DeckModificationEvent

	case class LockedCardSlotAdded(entity : Entity, slot : LockedCardSlot) extends GameEvent with DeckModificationEvent

	case class LockedCardResolved(entity : Entity, lockedCard : LockedCard) extends GameEvent with DeckModificationEvent

	case class AttachedCardsChanged(entity : Entity, card : Entity, attachmentKey : AnyRef) extends GameEvent
}
