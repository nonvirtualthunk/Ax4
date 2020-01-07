package arx.ax4.game.components

import arx.Prelude._
import arx.ax4.game.entities.Companions.{CharacterInfo, Item, Weapon}
import arx.ax4.game.entities.cardeffects._
import arx.ax4.game.entities.{CardTypes, CharacterInfo, Item, Weapon}
import arx.ax4.game.event.EntityCreated
import arx.ax4.game.logic.{CardLogic, IdentityLogic}
import arx.core.introspection.FieldOperations._
import arx.core.math.Sext
import arx.core.units.UnitOfTime
import arx.engine.game.components.GameComponent
import arx.engine.world.World

class CardCreationComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(world: World): Unit = {
		implicit val w = world
		implicit val view = world.view

		onGameEventEndWithPrecedence(5) {
			case EntityCreated(entity) =>
				for (weapon <- entity.dataOpt[Weapon]) {
					for ((key, attack) <- weapon.attacks; _ <- 0 until attack.cardCount) yield {
						val card = CardLogic.createCard(entity, CD => {
							CD.cardType = CardTypes.AttackCard
							CD.name = attack.name
							CD.effects = Vector(AttackGameEffect(key, attack.copy(weapon = entity)))
							CD.costs = Vector(PayActionPoints(attack.actionCost), PayStamina(attack.staminaCost))
							CD.source = entity
						})
						if (entity.hasData[Item]) { world.modify(entity, Item.equippedCards append card) }
						world.modify(entity, Weapon.attackCards append card)
					}
				}

				for (charInfo <- entity.dataOpt[CharacterInfo]) {
					val moveCards = for (_ <- 0 until 3) yield {
						CardLogic.createCard(entity, cd => {
							cd.costs = Vector(PayActionPoints(1))
							cd.effects = Vector(GainMovePoints(bonusMP = Sext(0)))
							cd.cardType = CardTypes.MoveCard
							cd.name = "Move"
						})
					}

					val gatherCards = for (_ <- 0 until 1) yield {
						CardLogic.createCard(entity, cd => {
							cd.costs = Vector(PayActionPoints(2))
							cd.name = "Gather"
							cd.cardType = CardTypes.GatherCard
							cd.effects = Vector(GatherCardEffect(1))
						})
					}

					world.modify(entity, CharacterInfo.innateCards -> (moveCards ++ gatherCards).toVector)
				}

				// Initialize the equipped and inventory cards for items based on their data
				for (item <- entity.dataOpt[Item]) {
					// if this item is a weapon, add attack cards corresponding to its various attacks
					if (item.equipable) {
						val card = CardLogic.createCard(entity, CD => {
							CD.cardType = CardTypes.ItemCard
							CD.name = s"Equip ${IdentityLogic.name(entity).capitalizeAll}"
							CD.effects = Vector(EquipItemEffect(entity))
							CD.costs = Vector(PayActionPoints(1))
						})
						world.modify(entity, Item.inventoryCards append card)
					} else {
						val card = CardLogic.createCard(entity, CD => {
							CD.cardType = CardTypes.ItemCard
							CD.name = IdentityLogic.name(entity).capitalizeAll
							CD.cardType = CardTypes.ItemCard
						})
						world.modify(entity, Item.inventoryCards append card)
					}

					item.inventoryCardArchetypes.foreach(arch => world.modify(entity, Item.inventoryCards append arch.createEntity(world)))
					item.equippedCardArchetypes.foreach(arch => world.modify(entity, Item.equippedCards append arch.createEntity(world)))
				}



		}
	}
}
