package arx.ax4.game.components

import arx.ax4.game.entities.Companions.CardData
import arx.ax4.game.entities.cardeffects.{GainMovePoints, PayActionPoints}
import arx.ax4.game.entities.{CardData, CardInDeck, CardTypes, CharacterInfo, DeckData, Item, Weapon}
import arx.ax4.game.event.CardEvents.AttachedCardsChanged
import arx.ax4.game.event.{DeckModificationEvent, EntityCreated, EntityPlaced, EquipItem, TransferItem, UnequipItem}
import arx.ax4.game.logic.CardAdditionStyle.{DrawDiscardSplit, DrawPile}
import arx.ax4.game.logic.{CardAdditionStyle, CardLogic, IdentityLogic}
import arx.core.introspection.FieldOperations.MapField
import arx.core.math.Sext
import arx.core.units.UnitOfTime
import arx.engine.entity.Entity
import arx.engine.event.GameEvent
import arx.engine.game.components.GameComponent
import arx.engine.world.{World, WorldView}

class DeckComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		implicit val implWorld = world
		onGameEventEnd {
			case EquipItem(entity, item) =>
				for (deck <- entity.dataOpt[DeckData]) {
					val ID = item[Item]
					// add all the cards for the item being equipped
					ID.equippedCards.foreach(card => CardLogic.addCard(entity, card, CardAdditionStyle.DrawDiscardSplit))
					// if we add an attack card from an item, then remove all natural attack cards
					if (ID.equippedCards.exists(c => IdentityLogic.isA(c, CardTypes.AttackCard))) {
						for (naturalAttackCard <- deck.allCards.filter(c => CardLogic.isNaturalAttackCard(c))) {
							CardLogic.removeCard(entity, naturalAttackCard)
						}
					}
					// if we added any cards, shuffle the draw pile
					if (ID.equippedCards.nonEmpty) {
						CardLogic.shuffleDrawPile(entity)
					}
				}
			case UnequipItem(entity, item) =>
				for (deck <- entity.dataOpt[DeckData]) {
					val ID = item[Item]
					// add all the cards for the item being equipped
					ID.equippedCards.foreach(card => CardLogic.removeCard(entity, card))

					val hasNaturalAttacks = deck.allCards.exists(c => CardLogic.isNaturalAttackCard(c))
					val hasWeaponAttacks = deck.allCards.exists(c => CardLogic.isWeaponAttackCard(c))
					if (!hasWeaponAttacks && !hasNaturalAttacks) {
						addNaturalAttackCards(entity)
					}
				}
			case TransferItem(item, from, to) =>
				for (itemData <- item.dataOpt[Item]) {
					from match {
						case Some(entity) if entity.hasData[DeckData] => itemData.inventoryCards.foreach(c => CardLogic.removeCard(entity, c))
						case _ => // do nothing
					}
					to match {
						case Some(entity) if entity.hasData[DeckData] && itemData.inventoryCards.nonEmpty =>
							itemData.inventoryCards.foreach(c => CardLogic.addCard(entity, c, CardAdditionStyle.DrawDiscardSplit))
							CardLogic.shuffleDrawPile(entity)
						case _ => // do nothing
					}
				}
			case EntityCreated(entity) =>
				if (entity.hasData[DeckData]) {
					initializeDeck(entity)
				}
			case dm : DeckModificationEvent =>
				reconcileAttachments()
			case ge : GameEvent =>

		}
	}

	def initializeDeck(entity: Entity)(implicit world : World, view : WorldView): Unit = {
		for (charInfo <- entity.dataOpt[CharacterInfo]; card <- charInfo.innateCards) {
			CardLogic.addCard(entity, card, DrawPile)
		}

		addNaturalAttackCards(entity)
	}

	def addNaturalAttackCards(entity : Entity)(implicit world : World, view : WorldView): Unit = {
		for (weapon <- entity.dataOpt[Weapon] if weapon.naturalWeapon ; card <- weapon.attackCards) {
			CardLogic.addCard(entity, card, DrawDiscardSplit)
		}
	}

	def reconcileAttachments()(implicit world : World, view : WorldView): Unit = {
		for (deckEnt <- view.entitiesWithData[DeckData]) {
			val deck = deckEnt[DeckData]

			val availableCardSet = deck.allNonExhaustedCards.toSet
			for (card <- deck.allCards) {
				val cd = card[CardData]
				for ((key, attachedCards) <- cd.attachedCards) {
					val cardsToRemove = attachedCards.filterNot(availableCardSet.contains)
					cardsToRemove.foreach(toRemove => CardLogic.detachCard(deckEnt, card, key, toRemove))
				}

				for ((key,attachment) <- cd.attachments) {
					// if it's an automatic attachment, but does not have the full complement of attached cards, find some
					val currentlyAttached = cd.attachedCards.getOrElse(key, Vector())
					val missingCount = attachment.count - currentlyAttached.size
					if (attachment.automaticAttachment && missingCount > 0) {
						val possibleAttachments = availableCardSet
							.filterNot(currentlyAttached.contains)
   						.filter(card => attachment.condition.forall(condition => condition.isTrueFor(view, CardInDeck(deckEnt, card))))
   						.toVector

						val newAttachments = possibleAttachments.take(missingCount)
						newAttachments.foreach(newCard => CardLogic.attachCard(deckEnt, card, key, newCard))
					}
				}
			}

			deck.allAvailableCards
		}
	}
}
