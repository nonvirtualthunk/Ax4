package arx.ax4.game.components

import arx.ax4.game.entities.Companions.{Item, Weapon}
import arx.ax4.game.entities.cardeffects.{AttackCardEffect, EquipItemEffect, PayActionPoints, PayAttackActionPoints, PayAttackStaminaPoints}
import arx.ax4.game.entities.{AttackReference, CardTypes, Item, Weapon}
import arx.ax4.game.event.EntityCreated
import arx.ax4.game.logic.{CardLogic, IdentityLogic}
import arx.core.units.UnitOfTime
import arx.engine.game.components.GameComponent
import arx.engine.world.World
import arx.core.introspection.FieldOperations._

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
						val ref = AttackReference(entity, key, None, None)
						val card = CardLogic.createCard(entity, CD => {
							CD.cardType = CardTypes.AttackCard
							CD.name = attack.name
							CD.effects = Vector(AttackCardEffect(ref))
							CD.costs = Vector(PayAttackActionPoints(ref), PayAttackStaminaPoints(ref))
							CD.source = entity
						})
						if (entity.hasData[Item]) { world.modify(entity, Item.equippedCards append card) }
						world.modify(entity, Weapon.attackCards append card)
					}
				}

				// Initialize the equipped and inventory cards for items based on their data
				for (item <- entity.dataOpt[Item]) {
					// if this item is a weapon, add attack cards corresponding to its various attacks
					if (item.equipable) {
						val card = CardLogic.createCard(entity, CD => {
							CD.cardType = CardTypes.ItemCard
							CD.name = s"Equip ${IdentityLogic.name(entity)}"
							CD.effects = Vector(EquipItemEffect(entity))
							CD.costs = Vector(PayActionPoints(1))
							CD.source = entity
						})
						world.modify(entity, Item.inventoryCards append card)
					}
				}

		}
	}
}
