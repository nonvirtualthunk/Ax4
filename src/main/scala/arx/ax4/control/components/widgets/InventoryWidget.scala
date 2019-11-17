package arx.ax4.control.components.widgets

import arx.ax4.game.logic.{IdentityLogic, InventoryLogic}
import arx.ax4.graphics.data.SpriteLibrary
import arx.engine.control.components.windowing.{Widget, WidgetInstance}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.graphics.{Image, ScaledImage}
import arx.graphics.helpers.Color
import arx.Prelude._
import arx.ax4.game.entities.Consumable

class InventoryWidget(parent : Widget) extends WidgetInstance {

	val widget = parent.createChild("InventoryWidgets.InventoryWidget")

	def updateBindings(implicit view : WorldView, display : World, selC : Entity): Unit = {
		widget.bind("items", () => InventoryLogic.heldItems(selC).map(item => {
			SimpleItemDisplayInfo(
				IdentityLogic.name(item).capitalizeAll,
				ScaledImage.scaleToPixelWidth(SpriteLibrary.iconFor(IdentityLogic.kind(item)), 64),
				Color.White,
				item.hasData[Consumable])
		}))
	}
}


case class SimpleItemDisplayInfo(name : String, icon : ScaledImage, color : Color, consumable : Boolean)