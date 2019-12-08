package arx.ax4.control.components
import arx.ax4.game.entities.Companions.DeckData
import arx.ax4.graphics.data.TacticalUIData
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2f, Vec2i}
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.{BottomLeft, PositionExpression, TopLeft}
import arx.engine.control.event.{Mouse, MouseButton, MousePressEvent, MouseReleaseEvent}
import arx.engine.entity.Entity
import arx.engine.world.{HypotheticalWorldView, World}
import arx.resource.ResourceManager

class CardControl extends AxControlComponent {

	var cardWidgets : Map[Entity, Widget] = Map()
	var heldCard : Option[Entity] = None
	var grabOffset : ReadVec2i = Vec2i.Zero


	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val tuid = display[TacticalUIData]

		if (!Mouse.buttonDown.getOrElse(MouseButton.Left, false)) {
			heldCard = None
		}

		for (selC <- tuid.selectedCharacter) {
			val hand = selC(DeckData)(gameView).hand
			for ((card, index) <- hand.zipWithIndex) {
				if (!cardWidgets.contains(card)) {
					val cardWidget = tuid.mainSectionWidget.createChild("CardWidgets.CardWidget")
					cardWidget.y = PositionExpression.Constant(-400, BottomLeft)

					cardWidget.bind("card", () => {CardInfo(card)(gameView)})

					cardWidget.onEvent {
						case MousePressEvent(button, pos, modifiers) =>
							heldCard = Some(card)
							grabOffset = cardWidget.windowingSystem.currentWindowingMousePosition - cardWidget.drawing.absolutePosition.xy
						case MouseReleaseEvent(button, pos, modifiers) => heldCard = None
					}

					cardWidgets += card -> cardWidget
				}

				val toRemove = cardWidgets.filterKeys(c => ! hand.contains(c))
				for ((card, widget) <- toRemove) {
					widget.destroy()
					cardWidgets -= card
				}

				for ((card, widget) <- cardWidgets) {
					val index = hand.indexOf(card)

					widget.z = if (heldCard.contains(card) || (heldCard.isEmpty && widget.isUnderCursor)) {
						100
					} else {
						index
					}
					val desiredY = heldCard match {
						case Some(held) =>
							if (card == held) {
								widget.windowingSystem.currentWindowingMousePosition.y - grabOffset.y
							} else {
								widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y - 200
							}
						case None =>
							if (widget.isUnderCursor) {
								widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y - widget.drawing.effectiveDimensions.y
							} else {
								widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y - 200
							}
					}
					val desiredX = if (heldCard.contains(card)) {
						widget.windowingSystem.currentWindowingMousePosition.x - grabOffset.x
					} else {
						widget.parent.drawing.absolutePosition.x + 100 + 225 * index
					}

					val curY = widget.position.y match {
						case PositionExpression.Absolute(value, _) => value
						case _ => desiredY
					}

					val curX = widget.position.x match {
						case PositionExpression.Absolute(value, _) => value
						case _ => desiredX
					}

					val delta = (Vec2f(desiredX, desiredY) - Vec2f(curX, curY))
					val deltaN = delta.normalizeSafe * (delta.lengthSafe * 0.035f).max(15.0f)

					val deltaY = deltaN.y.toInt.abs
					val newY = if (desiredY > curY) {
						(curY + deltaY).min(desiredY)
					} else if (desiredY < curY) {
						(curY - deltaY).max(desiredY)
					} else {
						curY
					}

					val deltaX = deltaN.x.toInt.abs
					val newX = if (desiredX > curX) {
						(curX + deltaX).min(desiredX)
					} else if (desiredX < curX) {
						(curX - deltaX).max(desiredX)
					} else {
						curX
					}

					widget.x = PositionExpression.Absolute(newX, TopLeft)
					widget.y = PositionExpression.Absolute(newY, TopLeft)
					widget.drawing.backgroundImage = if (heldCard.contains(card) && curY < (widget.parent.drawing.effectiveDimensions.y - 1000)) {
						Some(ResourceManager.image("graphics/ui/active_card_border.png"))
					} else {
						Some(ResourceManager.image("graphics/ui/card_border.png"))
					}
				}
			}
		}
	}

	override protected def onInitialize(view: HypotheticalWorldView, game: World, display: World): Unit = {
		val tuid = display[TacticalUIData]
	}
}
