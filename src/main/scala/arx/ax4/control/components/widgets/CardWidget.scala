package arx.ax4.control.components.widgets

import arx.ax4.control.components.{CardControl, CardInfo}
import arx.ax4.game.entities.{CardData, CardLibrary, CardTypes, EntityArchetype, TagData}
import arx.ax4.game.entities.Companions.{CardData, TagData}
import arx.core.vec.Vec2i
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.{PositionExpression, TopLeft}
import arx.engine.control.components.windowing.widgets.data.{OverlayData, WidgetOverlay}
import arx.engine.control.event.{MousePressEvent, MouseReleaseEvent}
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.WorldView
import arx.graphics.TToImage
import arx.graphics.helpers.RGBA
import arx.resource.ResourceManager

object CardWidget {
	def apply(parent : Widget, selC : Entity, card : Entity)(implicit view : WorldView) : Widget = {
		CardWidget(parent, card.data[CardData], () => CardInfo(selC, card))
	}

	def apply(parent : Widget, selC : Option[Entity], cardArch : Taxon)(implicit view : WorldView) : Widget = {
		val arch = CardLibrary.withKind(cardArch)
		val CD : CardData = arch.data(CardData)
		val TD : TagData = arch.data(TagData)
		CardWidget(parent, CD, () => CardInfo(selC, CD, TD)(view))
	}

	def apply(parent : Widget, cardData : CardData, cardInfo : () => CardInfo) : Widget = {
		val cardWidget = parent.createChild("CardWidgets.CardWidget")
		cardWidget.drawing.backgroundImage = Some(cardData.cardType match {
			case CardTypes.ItemCard => ResourceManager.image("graphics/ui/item_card_border.png")
			case CardTypes.AttackCard => ResourceManager.image("graphics/ui/attack_card_border.png")
			case CardTypes.StatusCard => ResourceManager.image("graphics/ui/status_card_border.png")
			case _ => ResourceManager.image("graphics/ui/card_border_no_padding.png")
		})

		cardWidget.bind("card", cardInfo)

		cardWidget.attachData[OverlayData]
		val od = cardWidget[OverlayData]
		od.overlays += CardControl.LockedOverlay -> WidgetOverlay(
			drawOverlay = Moddable(false),
			overlayImage = Moddable(ResourceManager.image("graphics/ui/locked_overlay.png") : TToImage),
			overlayEdgeColor = Moddable(RGBA(0.3f,0.3f,0.3f,1.0f)),
			pixelScale = 2
		)
		od.overlays += CardControl.ActiveOverlay -> WidgetOverlay(
			drawOverlay = Moddable(false),
			overlayImage = Moddable(ResourceManager.image("graphics/ui/active_card_overlay.png") : TToImage),
			overlayEdgeColor = Moddable(RGBA(1.0f,1.0f,1.0f,1.0f)),
			pixelScale = 2,
			pixelSizeDelta = Vec2i(3,3)
		)

		cardWidget
	}
}
