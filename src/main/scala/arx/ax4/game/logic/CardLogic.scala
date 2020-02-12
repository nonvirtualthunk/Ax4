package arx.ax4.game.logic

import arx.Prelude.toArxVector
import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.Companions.{CardData, DeckData}
import arx.ax4.game.entities.cardeffects.{AttackGameEffect, GameEffect}
import arx.ax4.game.entities._
import arx.ax4.game.event.CardEvents._
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.game.logic.Randomizer

import scala.collection.GenTraversableOnce

object CardLogic {
	import arx.core.introspection.FieldOperations._

	def drawHand(entity: Entity)(implicit world: World) = {
		implicit val view = world.view
		val DD = entity[DeckData]

		resolveLockedCards(entity)
		val lockedSlots = DD.lockedCards

		world.startEvent(HandDrawn(entity))
		for (i <- 0 until DD.drawCount) {
			if (lockedSlots.size > i && !lockedSlots(i).resolvedCard.isSentinel && !lockedSlots(i).resolvedCard[CardData].exhausted) {
				val lockedCard = lockedSlots(i).resolvedCard
				drawSpecificCardFromDrawOrDiscard(entity, lockedCard)
			} else {
				drawCard(entity)
			}
		}
		world.endEvent(HandDrawn(entity))
	}

	def reshuffle(entity: Entity)(implicit world: World) = {
		val DD = world.view.data[DeckData](entity)
		world.startEvent(DeckShuffled(entity))
		world.eventStmt(DrawPileShuffled(entity)) {
			world.modify(entity, DeckData.drawPile -> shuffle(DD.discardPile))
		}
		world.modify(entity, DeckData.discardPile -> Vector())
		world.endEvent(DeckShuffled(entity))
	}

	def shuffleDrawPile(entity: Entity)(implicit world: World) = {
		val DD = world.view.data[DeckData](entity)
		world.eventStmt(DrawPileShuffled(entity)) {
			world.modify(entity, DeckData.drawPile -> shuffle(DD.drawPile))
		}
	}

	def shuffle(cards: Vector[Entity])(implicit world: World): Vector[Entity] = {
		val randomizer = Randomizer(world)

		var remainingCards = cards
		var ret = Vector[Entity]()
		while (remainingCards.nonEmpty) {
			val nextIndex = randomizer.nextInt(remainingCards.size)
			ret :+= remainingCards(nextIndex)
			// swap and pop
			remainingCards = remainingCards.updated(nextIndex, remainingCards(0))
			remainingCards = remainingCards.tail
		}

		ret
	}

	def drawCard(entity: Entity)(implicit world: World): Unit = {
		implicit val view = world.view
		val CD = entity[DeckData]

		if (CD.drawPile.isEmpty) {
			reshuffle(entity)
		}

		if (CD.drawPile.nonEmpty) {
			val drawn = CD.drawPile.head
			world.startEvent(CardDrawn(entity, drawn))
			world.modify(entity, DeckData.drawPile.popFront())
			world.modify(entity, DeckData.hand.append(drawn))
			world.endEvent(CardDrawn(entity, drawn))
		} else {
			Noto.warn("no cards in deck, cannot draw")
		}
	}

	def drawSpecificCardFromDrawOrDiscard(entity: Entity, card : Entity)(implicit world: World): Unit = {
		implicit val view = world.view
		val DD = entity[DeckData]

		if (DD.drawPile.contains(card)) {
			world.modify(entity, DeckData.drawPile remove card)
		} else if (DD.discardPile.contains(card)) {
			world.modify(entity, DeckData.discardPile remove card)
		}

		world.startEvent(CardDrawn(entity, card))
		world.modify(entity, DeckData.hand.append(card))
		world.endEvent(CardDrawn(entity, card))
	}

	def discardHand(entity: Entity, explicit: Boolean)(implicit world: World): Unit = {
		val hand = world.view.data[DeckData](entity).hand
		world.startEvent(HandDiscarded(entity, hand))
		discardCards(entity, world.view.data[DeckData](entity).hand, false)
		world.endEvent(HandDiscarded(entity, hand))
	}

	def discardCards(entity: Entity, cards: Seq[Entity], explicit: Boolean)(implicit world: World): Unit = {
		implicit val view = world.view

		var remaining = cards
		while (remaining.nonEmpty) {
			val card = remaining.head
			world.startEvent(CardDiscarded(entity, card, explicitDiscard = explicit))
			world.modify(entity, DeckData.discardPile append card)
			world.modify(entity, DeckData.hand remove card)
			world.endEvent(CardDiscarded(entity, card, explicitDiscard = explicit))
			remaining = remaining.tail
		}
	}

	def removeCard(entity: Entity, card: Entity)(implicit world: World): Unit = {
		implicit val view = world.view
		val deck = entity[DeckData]

		if (deck.containsCard(card)) {
			world.startEvent(CardRemoved(entity, card))

			// detach all attached cards before removing the card itself
			detachedAllFromCard(entity, card)

			removeCardFromCurrentPile(entity, card)

			world.endEvent(CardRemoved(entity, card))
		}
	}

	def addCard(entity: Entity, card: Entity, style: CardAdditionStyle)(implicit world: World): Unit = {
		implicit val view = world.view
		val deck = entity[DeckData]

		if (!card.hasData[CardData]) {
			Noto.error("Trying to add non-card to deck")
		}

		val weightDrawPile = style match {
			case CardAdditionStyle.DiscardPile => 0
			case _ => deck.drawPile.size
		}
		val weightDiscardPile = style match {
			case CardAdditionStyle.DrawPile => 0
			case _ => deck.discardPile.size
		}

		world.startEvent(CardAdded(entity, card))
		if (style == CardAdditionStyle.Hand) {
			world.modify(entity, DeckData.hand append card)
		} else {
			val randomizer = Randomizer(world)
			if (randomizer.nextInt(weightDrawPile + weightDiscardPile) < weightDrawPile) {
				world.modify(entity, DeckData.drawPile append card)
			} else {
				world.modify(entity, DeckData.discardPile append card)
			}
		}
		world.endEvent(CardAdded(entity, card))
	}

	def playCard(entity: Entity, card: Entity, cardPlayInstance: CardPlayInstance, selections: SelectionResult)(implicit world: World): Unit = {
		implicit val view = world.view

		world.startEvent(CardPlayed(entity, card))

		world.modify(entity, DeckData.hand remove card)
		world.modify(entity, DeckData.discardPile append card)

		val CD = card[CardData]
		for ((_, cost) <- cardPlayInstance.costs) {
			cost.applyEffect(world, selections)
		}

		for ((_, effect) <- cardPlayInstance.effects) {
			effect.applyEffect(world, selections)
		}

		world.endEvent(CardPlayed(entity, card))
	}

	def createCard(source: Entity, arch : EntityArchetype)(implicit world: World): Entity = {
		val card = arch.createEntity(world)
		card
	}

	def createCard(source: Entity, cardInit: CardData => Unit)(implicit world: World): Entity = {
		val ent = world.createEntity()
		val CD = new CardData
		cardInit(CD)
		CD.source = source
		ent.attach(CD) in world
		ent
	}


	def isCardLocked(source: Entity, card: Entity)(implicit view: WorldView): Boolean = source.dataOpt[DeckData] match {
		case Some(dd) => dd.lockedCards.exists(l => l.resolvedCard == card)
		case _ => false
	}

	def resolveLockedCards(entity : Entity)(implicit world : World): Unit = {
		implicit val view = world.view
		val dd = entity[DeckData]

		for ((lockedCard,index) <- dd.lockedCards.zipWithIndex if ! dd.allCards.contains(lockedCard.resolvedCard)) {
			world.modify(entity, DeckData.lockedCards -> dd.lockedCards.updated(index, lockedCard.copy(resolvedCard = Entity.Sentinel)))
		}

		for ((lockedCard,index) <- dd.lockedCards.zipWithIndex if lockedCard.resolvedCard.isSentinel) {
			val resolved = resolveLockedCard(entity, lockedCard.locked)
			if (lockedCard.resolvedCard != resolved) {
				world.eventStmt(LockedCardResolved(entity, lockedCard)) {
					world.modify(entity, DeckData.lockedCards -> dd.lockedCards.updated(index, lockedCard.copy(resolvedCard = resolved)))
				}
			}
		}
	}

	def resolveLockedCard(source: Entity, lockedCard: LockedCardType)(implicit view: WorldView): Entity = {
		lockedCard match {
			case LockedCardType.SpecificCard(card) => card
				// TODO: Special attacks and locked card interaction
			case LockedCardType.MetaAttackCard(attackKey, specialAttack) => source[DeckData].allAvailableCards.find(c =>
				c[CardData].effects.exists {
					case AttackGameEffect(key, attackData) => key == attackKey
					case _ => false
				}).getOrElse(Entity.Sentinel)
			case LockedCardType.Empty => Entity.Sentinel
		}
	}

	def addLockedCardSlot(source : Entity, slot : LockedCardSlot)(implicit world : World) : Unit = {
		world.startEvent(LockedCardSlotAdded(source, slot))
		world.modify(source, DeckData.lockedCardSlots append slot)
		world.modify(source, DeckData.lockedCards append LockedCard(LockedCardType.Empty, Entity.Sentinel))
		world.endEvent(LockedCardSlotAdded(source, slot))
	}

	def setLockedCard(source: Entity, index: Int, lockedCard: LockedCardType)(implicit world: World): Unit = {
		implicit val v = world.view
		source.dataOpt[DeckData] match {
			case Some(dd) =>
				// TODO: locked cards doesn't always have sufficient space in the vector
				val newLockedSeq = dd.lockedCards.updated(index, LockedCard(lockedCard, resolveLockedCard(source, lockedCard)))
				world.startEvent(LockedCardChanged(source, index, lockedCard))
				world.modify(source, DeckData.lockedCards -> newLockedSeq)
				world.endEvent(LockedCardChanged(source, index, lockedCard))
			case _ => Noto.warn(s"Trying to set locked card slot on entity without a deck: $source")
		}
	}

	def isNaturalAttackCard(card: Entity)(implicit view: WorldView): Boolean = {
		isAttackCard(card, isNaturalWeapon = true)
	}

	def isWeaponAttackCard(card: Entity)(implicit view: WorldView): Boolean = {
		isAttackCard(card, isNaturalWeapon = false)
	}

	def isAttackCard(card: Entity, isNaturalWeapon : Boolean)(implicit view: WorldView): Boolean = {
		val cd = card[CardData]
		cd.cardType == CardTypes.AttackCard &&
			(cd.source.dataOpt[Weapon] match {
				case Some(weapon) => weapon.naturalWeapon == isNaturalWeapon
				case None => false
			})
	}

	def isPlayable(card: Entity)(implicit view : WorldView): Boolean = {
		val (costs,effects) = effectiveCostsAndEffects(card)
		costs.nonEmpty || effects.nonEmpty
	}

	private def removeCardFromCurrentPile(entity : Entity, card : Entity)(implicit world : World) = {
		implicit val view = world.view

		val deck = entity[DeckData]
		if (deck.drawPile.contains(card)) {
			world.modify(entity, DeckData.drawPile remove card)
		} else if (deck.discardPile.contains(card)) {
			world.modify(entity, DeckData.discardPile remove card)
		} else if (deck.hand.contains(card)) {
			world.modify(entity, DeckData.hand remove card)
		} else if (deck.exhaustPile.contains(card)) {
			world.modify(entity, DeckData.exhaustPile remove card)
		} else if (deck.attachedCards.contains(card)) {
			world.modify(entity, DeckData.attachedCards remove card)
		} else {
			Noto.warn(s"Could not find card to remove from pile in any supported pile : $entity, $card")
		}
	}

	def attachCard(entity : Entity, attachTo : Entity, key : AnyRef, attached : Entity)(implicit world : World) : Unit = {
		implicit val view = world.view
		val currentAttachment = attachTo[CardData].attachedCards.getOrElse(key, Vector())

		world.eventStmt(AttachedCardsChanged(entity, attachTo, key)) {
			world.modify(attached, CardData.attachedTo + attachTo)
			world.modify(attachTo, CardData.attachedCards.put(key, currentAttachment :+ attached))
			removeCardFromCurrentPile(entity, attached)
			world.modify(entity, DeckData.attachedCards append attached)
		}
	}

	def detachCard(entity : Entity, attachedTo : Entity, key : AnyRef, toRemove : Entity)(implicit world : World) : Unit = {
		implicit val view = world.view
		val currentAttachment = attachedTo[CardData].attachedCards.getOrElse(key, Vector())
		if (currentAttachment.contains(toRemove)) {
			val deck = entity[DeckData]
			world.eventStmt(AttachedCardsChanged(entity, attachedTo, key)) {
				world.modify(toRemove, CardData.attachedTo - attachedTo)
				world.modify(attachedTo, CardData.attachedCards.put(key, currentAttachment without toRemove))
				// if the detached card is in the attachedCards section, move it to discard
				if (deck.attachedCards.contains(toRemove)) {
					world.modify(entity, DeckData.attachedCards remove toRemove)
					world.modify(entity, DeckData.discardPile append toRemove)
				} else {
					Noto.warn("Attached card was not in the attachedCards section")
				}
			}
		}
	}

	def detachedAllFromCard(entity : Entity, card : Entity)(implicit world : World) : Unit = {
		implicit val view = world.view
		card[CardData].attachedCards.foreach{ case (key, attachedV) => attachedV.foreach(a => detachCard(entity, card, key, a)) }
	}

	def effectiveCostsAndEffects(card : Entity)(implicit view : WorldView) : (Vector[GameEffect], Vector[GameEffect]) = {
		effectiveCostsAndEffects(card(CardData))
	}
	def effectiveCostsAndEffects(CD : CardData)(implicit view : WorldView) : (Vector[GameEffect], Vector[GameEffect]) = {
		var costs = CD.costs
		var effects = CD.effects

		for ((key,attachment) <- CD.attachments; attachedCards = CD.attachedCards.getOrElse(key, Vector())) {
			attachment.attachmentStyle match {
				case AttachmentStyle.Contained =>
				// do nothing, nothing specific happens to the contained card
				case AttachmentStyle.PlayModified(effectModifiers) =>
					// if this is a play-modified attachment, add all of the costs and effects from the attached card to this one
					for (attachedCardEnt <- attachedCards; attachedCard = attachedCardEnt[CardData]) {
						costs ++= attachedCard.costs.map(c => GameEffectModifier.applyAll(c, effectModifiers))
						effects ++= attachedCard.effects.map(c => GameEffectModifier.applyAll(c, effectModifiers))
					}
			}
		}

		(costs, effects)
	}

	def cardAndAllAttachments(card: Entity)(implicit view : WorldView): List[Entity] = {
		card :: card[CardData].attachedCards.values.flatten.flatMap(c => cardAndAllAttachments(c)).toList
	}
}


sealed trait CardAdditionStyle

object CardAdditionStyle {

	case object Hand extends CardAdditionStyle

	case object DrawPile extends CardAdditionStyle

	case object DiscardPile extends CardAdditionStyle

	case object DrawDiscardSplit extends CardAdditionStyle

}