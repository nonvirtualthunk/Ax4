package arx.ax4.game.components

import arx.ax4.game.entities.cardeffects.{GainMovePoints, PayActionPoints}
import arx.ax4.game.entities.{CardData, CardTypes, DeckData, Item, Weapon}
import arx.ax4.game.event.{EntityCreated, EntityPlaced, EquipItem, TransferItem, UnequipItem}
import arx.ax4.game.logic.CardAdditionStyle.{DrawDiscardSplit, DrawPile}
import arx.ax4.game.logic.{CardAdditionStyle, CardLogic}
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
					if (ID.equippedCards.exists(c => c[CardData].cardType == CardTypes.AttackCard)) {
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
			case ge : GameEvent =>

		}
	}

	def initializeDeck(entity: Entity)(implicit world : World, view : WorldView): Unit = {
		for (_ <- 0 until 3) {
			val moveCard = CardLogic.createCard(entity, cd => {
				cd.costs = Vector(PayActionPoints(1))
				cd.effects = Vector(GainMovePoints(bonusMP = Sext(0)))
				cd.cardType = CardTypes.MoveCard
				cd.name = "Move"
			})
			CardLogic.addCard(entity, moveCard, DrawPile)
		}

		addNaturalAttackCards(entity)
	}

	def addNaturalAttackCards(entity : Entity)(implicit world : World, view : WorldView): Unit = {
		for (weapon <- entity.dataOpt[Weapon] if weapon.naturalWeapon ; card <- weapon.attackCards) {
			CardLogic.addCard(entity, card, DrawDiscardSplit)
		}
	}
}
