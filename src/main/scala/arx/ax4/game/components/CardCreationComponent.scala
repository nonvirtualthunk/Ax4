package arx.ax4.game.components

import arx.Prelude._
import arx.ax4.game.entities.Companions.{CharacterInfo, Item, Weapon}
import arx.ax4.game.entities.cardeffects._
import arx.ax4.game.entities.{CardEffectGroup, CardLibrary, CardTypes, CharacterInfo, Item, Weapon}
import arx.ax4.game.event.EntityCreated
import arx.ax4.game.logic.{AllegianceLogic, CardLogic, IdentityLogic}
import arx.core.introspection.FieldOperations._
import arx.core.math.Sext
import arx.core.units.UnitOfTime
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, Taxonomy}
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

					for (i <- 0 until weapon.attackCardCount) {
						val kind = if (weapon.naturalWeapon) {
							CardTypes.NaturalAttackCard
						} else {
							CardTypes.AttackCard
						}

						val card = CardLogic.createCard(entity, kind, CD => {
							CD.name = "Strike"
							CD.cardEffectGroups = for ((key,attack) <- weapon.attacks.toVector.sortBy(_._1)) yield {
								val effGroup = new CardEffectGroup
								effGroup.name = Some(attack.name)
								effGroup.effects = Vector(AttackGameEffect(key, attack.copy(weapon = entity)))
								effGroup.costs = Vector(PayActionPoints(attack.actionCost), PayStamina(attack.staminaCost))
								effGroup
							}

							CD.source = entity
						})

						if (entity.hasData[Item]) { world.modify(entity, Item.equippedCards append card) }
						world.modify(entity, Weapon.attackCards append card)
					}
				}

				for (charInfo <- entity.dataOpt[CharacterInfo]) {
					var innateCards = Seq[Entity]()
					val moveCards = for (_ <- 0 until 3) yield {
						CardLogic.createCard(entity, CardLibrary.withKind(Taxonomy("CardTypes.move")))
					}

					innateCards ++= moveCards

					if (AllegianceLogic.isPlayerCharacter(entity)) {
						val gatherCards = for (_ <- 0 until 1) yield {
							CardLogic.createCard(entity, CardLibrary.withKind(Taxonomy("CardTypes.gather")))
						}
						innateCards ++= gatherCards
					}

					world.modify(entity, CharacterInfo.innateCards append innateCards.toVector)
				}

				// Initialize the equipped and inventory cards for items based on their data
				for (item <- entity.dataOpt[Item]) {
					// if this item is a weapon, add attack cards corresponding to its various attacks
					if (item.equipable) {
						val card = CardLogic.createCard(entity, CardTypes.ItemCard, CD => {
							CD.name = s"Equip ${IdentityLogic.name(entity).capitalizeAll}"
							val effGroup = new CardEffectGroup
							effGroup.effects = Vector(EquipItemEffect(entity))
							effGroup.costs = Vector(PayActionPoints(1))
							CD.cardEffectGroups = Vector(effGroup)
						})
						world.modify(card, IdentityData.kind -> Taxonomy("CardTypes.ItemCard"))
						world.modify(entity, Item.inventoryCards append card)
					} else {
						val card = CardLogic.createCard(entity, CardTypes.ItemCard, CD => {
							CD.name = IdentityLogic.name(entity).capitalizeAll
						})
						world.modify(entity, Item.inventoryCards append card)
					}

					item.inventoryCardArchetypes.foreach(arch => world.modify(entity, Item.inventoryCards append arch.createEntity(world)))
					item.equippedCardArchetypes.foreach(arch => world.modify(entity, Item.equippedCards append arch.createEntity(world)))
				}



		}
	}
}
