package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.Companions.DeckData
import arx.ax4.game.entities.{CardData, DeckData}
import arx.ax4.game.event.CardEvents.{CardDiscarded, CardDrawn, CardPlayed, DeckShuffled}
import arx.engine.entity.Entity
import arx.engine.world.World
import arx.game.logic.Randomizer

object CardLogic {
	import arx.core.introspection.FieldOperations._

	def drawHand(entity: Entity)(implicit world : World) = {
		implicit val view = world.view
		val DD = entity[DeckData]

		for (_ <- 0 until DD.drawCount) {
			drawCard(entity)
		}
	}

	def reshuffle(entity : Entity)(implicit world : World) = {
		val DD = world.view.data[DeckData](entity)
		world.startEvent(DeckShuffled(entity))
		world.modify(entity, DeckData.drawPile -> shuffle(DD.discardPile))
		world.modify(entity, DeckData.discardPile -> Vector())
		world.endEvent(DeckShuffled(entity))
	}

	def shuffle(cards : Vector[Entity])(implicit world : World) : Vector[Entity] = {
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

	def drawCard(entity : Entity)(implicit world : World) : Unit = {
		implicit val view = world.view
		val CD = entity[DeckData]

		if (CD.drawPile.isEmpty) {
			reshuffle(entity)
		}

		if (CD.drawPile.nonEmpty) {
			val drawn = CD.drawPile.head
			world.eventStmt(CardDrawn(entity, drawn)) {
				world.modify(entity, DeckData.drawPile.popFront())
				world.modify(entity, DeckData.hand.append(drawn))
			}
		} else {
			Noto.warn("no cards in deck, cannot draw")
		}
	}

	def discardHand(entity : Entity, explicit : Boolean)(implicit world : World) : Unit = {
		discardCards(entity, world.view.data[DeckData](entity).hand, false)
	}

	def discardCards(entity : Entity, cards : Seq[Entity], explicit : Boolean)(implicit world : World): Unit = {
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

	def playCard(entity : Entity, card : Entity, selections : SelectionResult)(implicit world : World) : Unit = {
		implicit val view = world.view

		world.startEvent(CardPlayed(entity, card))

		val CD = card[CardData]
		for (cost <- CD.costs) {
			if (!cost.canApplyEffect) {
				Noto.error("Played a card when one if its costs could no longer be paid")
			}
			cost.applyEffect(world, entity, selections)
		}

		for (effect <- CD.effects) {
			if (effect.canApplyEffect(view, entity)) {
				effect.applyEffect(world, entity, selections)
			} else {
				Noto.info("Effect was no longer applicable")
			}
		}

		world.modify(entity, DeckData.discardPile append card)
		world.modify(entity, DeckData.hand remove card)
		world.endEvent(CardPlayed(entity, card))
	}
}
