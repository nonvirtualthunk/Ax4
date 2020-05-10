package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.{Equipment, Inventory, Item, TagData}
import arx.ax4.game.entities.{Equipment, Inventory, Item, TagData, Weapon}
import arx.ax4.game.event.{EquipItem, TransferItem, UnequipItem}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}

object InventoryLogic {
	import arx.core.introspection.FieldOperations._

	def equip (entity : Entity, item : Entity)(implicit world : World) : Boolean = {
		implicit val view = world.view

		removeFromContainingInventory(item)
		unequip(item)

		world.startEvent(EquipItem(entity, item))
		world.modify(entity, Equipment.equipped + item, "equipped")
		world.modify(item, Item.equippedTo -> Some(entity))
		for (td <- entity.dataOpt[TagData]; id <- item.dataOpt[Item]) {
			for ((flag, amount) <- id.equippedFlags) {
				world.modify(entity, TagData.flags.incrementKey(flag, amount))
			}
		}
		world.endEvent(EquipItem(entity, item))
		true
	}


	def removeFromContainingInventory(item : Entity)(implicit world : World) = {
		transferItem(item, None)
	}

	def transferItem(item : Entity, to : Option[Entity])(implicit world : World) = {
		implicit val view = world.view
		val curInventoryOpt = item(Item).heldIn
		if (curInventoryOpt != to) {
			world.startEvent(TransferItem(item, curInventoryOpt, to))
			for (curInventory <- curInventoryOpt) {
				world.modify(curInventory, Inventory.heldItems - item)
			}
			for (toInventory <- to) {
				world.modify(toInventory, Inventory.heldItems + item)
			}
			world.modify(item, Item.heldIn -> to)
			world.endEvent(TransferItem(item, curInventoryOpt, to))
		}
	}

	def unequip(item : Entity)(implicit world : World) = {
		implicit val view = world.view
		for (curEquippedTo <- item(Item).equippedTo) {
			world.startEvent(UnequipItem(curEquippedTo, item))
			world.modify(curEquippedTo, Equipment.equipped - item)
			world.modify(item, Item.equippedTo -> None)
			for (td <- curEquippedTo.dataOpt[TagData]; id <- item.dataOpt[Item]) {
				for ((flag, amount) <- id.equippedFlags) {
					world.modify(curEquippedTo, TagData.flags.decrementKey(flag, amount))
				}
			}
			world.endEvent(UnequipItem(curEquippedTo, item))
		}

	}

	def heldItems(entity : Entity)(implicit view : WorldView) : List[Entity] = {
		entity.dataOpt[Inventory] match {
			case Some(inv) => inv.heldItems.toList.sortBy(i => IdentityLogic.kind(i).name)
			case None => Nil
		}
	}

	def heldAndEquippedItems(entity : Entity)(implicit view : WorldView) : List[Entity] = {
		equippedItems(entity) ::: heldItems(entity)
	}

	def equippedItems(entity : Entity)(implicit view : WorldView) : List[Entity] = {
		entity.dataOpt[Equipment] match {
			case Some(equip) => equip.equipped.toList.sortBy(i => IdentityLogic.kind(i).name)
			case None => Nil
		}
	}

	def equippedWeapons(entity : Entity)(implicit view : WorldView) : Seq[Entity] = {
		equippedItems(entity).filter(_.hasData[Weapon])
	}
}
