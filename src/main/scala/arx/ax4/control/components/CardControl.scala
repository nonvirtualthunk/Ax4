package arx.ax4.control.components
import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.Companions.{CardData, DeckData}
import arx.ax4.game.entities.cardeffects.{PayActionPoints, PayAttackActionPoints, PayAttackStaminaPoints, PayStamina}
import arx.ax4.game.entities.{CardData, CardPlay, CardTypes}
import arx.ax4.game.logic.CardLogic
import arx.ax4.graphics.data.{CardImageLibrary, SpriteLibrary, TacticalUIData}
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2f, Vec2i}
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.data.{OverlayData, WidgetOverlay}
import arx.engine.control.components.windowing.widgets.{BottomLeft, PositionExpression, TopLeft}
import arx.engine.control.event.{Mouse, MouseButton, MousePressEvent, MouseReleaseEvent}
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, Taxonomy}
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.graphics.{Image, TToImage}
import arx.graphics.helpers.{Color, HorizontalPaddingSection, ImageSection, LineBreakSection, RGBA, RichText, RichTextRenderSettings, RichTextSection, TextSection}
import arx.resource.ResourceManager

class CardControl(selectionControl : SelectionControl) extends AxControlComponent {

	var cardWidgets : Map[Entity, Widget] = Map()
	var heldCard : Option[Entity] = None
	var grabOffset : ReadVec2i = Vec2i.Zero
	var useCardOnDrop = false


	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val tuid = display[TacticalUIData]

		if (!Mouse.buttonDown.getOrElse(MouseButton.Left, false)) {
			heldCard = None
		}

		val unselectedDepth = -450



		for (selC <- tuid.selectedCharacter) {
			val DD = selC(DeckData)(gameView)
			val hand = DD.hand
			for ((card, index) <- hand.zipWithIndex) {
				if (!cardWidgets.contains(card)) {
					val cardWidget = tuid.mainSectionWidget.createChild("CardWidgets.CardWidget")
					cardWidget.y = PositionExpression.Absolute(cardWidget.parent.drawing.absolutePosition.y + cardWidget.parent.drawing.effectiveDimensions.y + unselectedDepth, TopLeft)
					cardWidget.drawing.backgroundImage = Some(card(CardData)(gameView).cardType match {
						case CardTypes.ItemCard => ResourceManager.image("graphics/ui/item_card_border.png")
						case CardTypes.AttackCard => ResourceManager.image("graphics/ui/attack_card_border.png")
						case _ => ResourceManager.image("graphics/ui/card_border_no_padding.png")
					})


					cardWidget.bind("card", () => {
						CardInfo(selC, card)(gameView)
					})

					cardWidget.onEvent {
						case MousePressEvent(button, pos, modifiers) =>
							heldCard = Some(card)
							grabOffset = cardWidget.windowingSystem.currentWindowingMousePosition - cardWidget.drawing.absolutePosition.xy
						case MouseReleaseEvent(button, pos, modifiers) =>
							cardDropped(game, display)
							heldCard = None
					}

					cardWidget.attachData[OverlayData]
					val od = cardWidget[OverlayData]
					od.overlays += CardControl.LockedOverlay -> WidgetOverlay(
						overlayImage = ResourceManager.image("graphics/ui/locked_overlay.png"),
						overlayEdgeColor = Moddable(RGBA(0.3f,0.3f,0.3f,1.0f)),
						pixelScale = 2
					)
					od.overlays += CardControl.ActiveOverlay -> WidgetOverlay(
						drawOverlay = false,
						overlayImage = ResourceManager.image("graphics/ui/active_card_overlay.png"),
						overlayEdgeColor = Moddable(RGBA(1.0f,1.0f,1.0f,1.0f)),
						pixelScale = 2,
						pixelSizeDelta = Vec2i(3,3)
					)

					cardWidgets += card -> cardWidget
				}
			}

			val toRemove = cardWidgets.filterKeys(c => ! hand.contains(c))
			for ((card, widget) <- toRemove) {
				widget.destroy()
				cardWidgets -= card
			}

			for ((card, widget) <- cardWidgets) {
				val index = hand.indexOf(card)

				widget.z = if (heldCard.contains(card) || (heldCard.isEmpty && widget.isUnderCursor && Mouse.isInWindow)) {
					100
				} else {
					20 - index
				}
				val desiredY = heldCard match {
					case Some(held) =>
						if (card == held) {
							widget.windowingSystem.currentWindowingMousePosition.y - grabOffset.y
						} else {
							widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y + unselectedDepth
						}
					case None =>
						if (widget.isUnderCursor) {
							widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y - widget.drawing.effectiveDimensions.y + 20
						} else {
							widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y + unselectedDepth
						}
				}

				val selWidgetSpace = if (tuid.selectedCharacterInfoWidget.showing.resolve()) {
					tuid.selectedCharacterInfoWidget.drawing.effectiveDimensions.x
				} else {
					0
				}
				val availableSpace = (widget.parent.drawing.effectiveDimensions.x - selWidgetSpace) - 100
				val xGap = (availableSpace / (cardWidgets.size-1).max(1)).min((widget.drawing.effectiveDimensions.x * 0.75f).toInt)

				val desiredX = if (heldCard.contains(card)) {
					widget.windowingSystem.currentWindowingMousePosition.x - grabOffset.x
				} else {
					widget.parent.drawing.absolutePosition.x + 50 + xGap * index
				}

				val curY = widget.position.y match {
					case PositionExpression.Absolute(value, _) => value
					case _ => desiredY
				}

				val curX = widget.position.x match {
					case PositionExpression.Absolute(value, _) => value
					case _ => desiredX
				}


				val maxSpeed = if (heldCard.contains(card)) { 64.0f } else { 32.0f }
				val delta = (Vec2f(desiredX, desiredY) - Vec2f(curX, curY))
				val deltaN = delta.normalizeSafe * (delta.lengthSafe * 0.7f).max(0.0f).min(maxSpeed)

				val deltaY = deltaN.y.round.abs
				val newY = if (deltaY == 0) {
					desiredY
				} else if (desiredY > curY) {
					(curY + deltaY).min(desiredY)
				} else if (desiredY < curY) {
					(curY - deltaY).max(desiredY)
				} else {
					desiredY
				}

				val deltaX = deltaN.x.round.abs
				val newX = if (deltaX == 0) {
					desiredX
				} else if (desiredX > curX) {
					(curX + deltaX).min(desiredX)
				} else if (desiredX < curX) {
					(curX - deltaX).max(desiredX)
				} else {
					desiredX
				}

				val isLockedCard = DD.lockedCards.map(_.resolvedCard).contains(card)

				widget.x = PositionExpression.Absolute(newX, TopLeft)
				widget.y = PositionExpression.Absolute(newY, TopLeft)

				val od = widget[OverlayData]

				od.overlays(CardControl.ActiveOverlay).drawOverlay = if (heldCard.contains(card) && curY < (widget.parent.drawing.effectiveDimensions.y - 1000) && CardLogic.isPlayable(card)(game.view)) {
					useCardOnDrop = true
					true
				} else {
					if (heldCard.contains(card)) {
						useCardOnDrop = false
					}
					false
				}

				od.overlays(CardControl.LockedOverlay).drawOverlay = isLockedCard
			}
		}

		if (heldCard.isDefined) {
			Mouse.setVisible(false)
		} else {
			Mouse.setVisible(true)
		}
	}

	override protected def onInitialize(hypView: HypotheticalWorldView, game: World, display: World): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = game.view

		onControlEvent {
			case HexMouseReleaseEvent(_,_,_,_) => cardDropped(game, display)
		}
	}

	def cardDropped(game : World, display : World): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = game.view

		if (useCardOnDrop) {
			for (card <- heldCard ; selC <- tuid.selectedCharacter) {
				if (CardLogic.isPlayable(card)) {
					val cardPlay = CardPlay(card)
					cardPlay.instantiate(game.view, selC) match {
						case Left(cardPlayInst) =>
							selectionControl.changeSelectionTarget(game, display, cardPlay, cardPlayInst, (sc) => CardLogic.playCard(selC, card, cardPlayInst, sc.selectionResults)(game))
						case Right(msg) => Noto.error(s"Could not play card: $msg")
					}
				}
			}
		}
	}
}

object CardControl {
	val LockedOverlay = "Locked Overlay"
	val ActiveOverlay = "Active Overlay"
}

case class CardInfo(name : String, image : Image, mainCost : RichText, secondaryCost : RichText, effects : RichText)
object CardInfo {
	def apply(character : Entity, card : Entity)(implicit view : WorldView) : CardInfo = {
		val CD = card[CardData]

		val apCosts = CD.costs.collect {
			case PayActionPoints(ap) => ap
			case PayAttackActionPoints(ref) => ref.resolve().map(_.actionCost).getOrElse(0)
		}
		val staminaCosts = CD.costs.collect {
			case PayStamina(stamina) => stamina
			case PayAttackStaminaPoints(ref) => ref.resolve().map(_.staminaCost).getOrElse(0)
		}

		val mainCost =  if (apCosts.nonEmpty) {
			RichText(TextSection(apCosts.sum.toString) :: HorizontalPaddingSection(10) :: ImageSection(ResourceManager.image("graphics/ui/action_point.png"), 2.0f, Color.White) :: Nil)
		} else {
			RichText.Empty
		}

		val secondaryCost = if (staminaCosts.nonEmpty) {
			RichText(TextSection(staminaCosts.sum.toString) :: HorizontalPaddingSection(10) :: ImageSection(ResourceManager.image("graphics/ui/stamina_point_large.png"), 2.0f, Color.White) :: Nil)
		} else {
			RichText.Empty
		}

		val combinedEffectsSections = CD.effects.map(e => e.toRichText(view, character, RichTextRenderSettings()).sections)
   		.foldLeft(Seq[RichTextSection]())((t1, t2) => t1 ++ Seq(LineBreakSection(0)) ++ t2)
		val effectText = RichText(combinedEffectsSections)


		val cardImage : TToImage = CardImageLibrary.cardImageOpt(CD.name) match {
			case Some(img) => img
			case _ =>
				CD.cardType match {
					//			case CardTypes.GatherCard => "third-party/shikashiModified/pickaxe.png"
					case CardTypes.GatherCard => "graphics/card_images/gather.png"
					case CardTypes.AttackCard => "graphics/card_images/punch.png"
					case CardTypes.MoveCard => "graphics/card_images/move.png"
					case _ => "default/blank_transparent.png"
				}
		}

		CardInfo(CD.name.capitalize, cardImage, mainCost, secondaryCost, effectText)
	}
}

