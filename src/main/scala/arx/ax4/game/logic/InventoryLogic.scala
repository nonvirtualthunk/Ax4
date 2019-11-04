package arx.ax4.game.logic

import arx.ax4.game.entities.Companions.{Equipment, Inventory, Item}
import arx.ax4.game.event.{EquipItem, TransferItem, UnequipItem}
import arx.engine.entity.Entity
import arx.engine.world.World

object InventoryLogic {
	import arx.core.introspection.FieldOperations._

	def equip (entity : Entity, item : Entity)(implicit world : World) : Boolean = {
		removeFromContainingInventory(item)
		unequip(item)

		world.startEvent(EquipItem(entity, item))
		world.modify(entity, Equipment.equipped + item, "equipped")
		world.modify(item, Item.equippedTo -> Some(entity))
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
			world.modify(item, Item.heldIn -> None)
			world.endEvent(TransferItem(item, curInventoryOpt, to))
		}
	}

	def unequip(item : Entity)(implicit world : World) = {
		implicit val view = world.view
		for (curEquippedTo <- item(Item).equippedTo) {
			world.startEvent(UnequipItem(curEquippedTo, item))
			world.modify(curEquippedTo, Equipment.equipped - item)
			world.modify(item, Item.equippedTo -> None)
			world.endEvent(UnequipItem(curEquippedTo, item))
		}

	}
}
