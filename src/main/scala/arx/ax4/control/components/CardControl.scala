package arx.ax4.control.components
import java.util.concurrent.atomic.AtomicBoolean

import arx.application.Noto
import arx.ax4.control.components.widgets.{CardWidget, CardWidgetData}
import arx.ax4.game.entities.Companions.{CardData, DeckData, TagData}
import arx.ax4.game.entities.cardeffects.{PayActionPoints, PayStamina}
import arx.ax4.game.entities.{AttachmentStyle, CardData, CardPlay, CardTypes, TagData, TagLibrary}
import arx.ax4.game.logic.{CardLogic, TagLogic}
import arx.ax4.game.logic.CardLogic.CostsAndEffects
import arx.ax4.graphics.data.{CardImageLibrary, TacticalUIData}
import arx.core.datastructures.OneOrMore.fromSingle
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2f, Vec2i}
import arx.engine.control.components.windowing.Widget
import arx.engine.control.components.windowing.widgets.data.{OverlayData, WidgetOverlay}
import arx.engine.control.components.windowing.widgets.{ListItemMousedOver, ListItemSelected, PositionExpression, TopLeft}
import arx.engine.control.event.{KeyModifiers, KeyboardMirror, Mouse, MouseButton, MousePressEvent, MouseReleaseEvent}
import arx.engine.data.Moddable
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.graphics.helpers._
import arx.graphics.{Image, TToImage}
import arx.resource.ResourceManager

class CardControl(selectionControl : SelectionControl) extends AxControlComponent {

	var cardWidgets : Map[Entity, Widget] = Map()
	var heldCard : Option[Entity] = None
	var heldEffectGroup : Int = 0
	var selectedCard : Option[Entity] = None
	var grabOffset : ReadVec2i = Vec2i.Zero
	var useCardOnDrop = new AtomicBoolean(false)


	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = gameView

		if (!Mouse.buttonDown.getOrElse(MouseButton.Left, false)) {
			heldCard = None
		}

		val unselectedDepth = -450



		for (selC <- tuid.selectedCharacter) {
			val DD = selC(DeckData)(gameView)
			val effHand = DD.hand.filterNot(selectedCard.contains)
			val handAndAttachments = effHand.flatMap(c => CardLogic.cardAndAllAttachments(c)(gameView)).filterNot(selectedCard.contains)
			for (card <- handAndAttachments) {
				if (!cardWidgets.contains(card)) {
					val cardWidget = CardWidget(tuid.mainSectionWidget, selC, card)
					cardWidget.y = PositionExpression.Absolute(cardWidget.parent.drawing.absolutePosition.y + cardWidget.parent.drawing.effectiveDimensions.y + unselectedDepth, TopLeft)

					cardWidget.onEvent {
						case MousePressEvent(button, pos, modifiers) =>
							heldCard = Some(card)
							heldEffectGroup = cardWidget[CardWidgetData].activeGroup
							grabOffset = cardWidget.windowingSystem.currentWindowingMousePosition - cardWidget.drawing.absolutePosition.xy
						case MouseReleaseEvent(button, pos, modifiers) =>
							cardDropped(game, display)
							heldCard = None
						case ListItemMousedOver(_,index,_) =>
							cardWidget[CardWidgetData].activeGroup = index
						case ListItemSelected(_, index, _) =>
							cardWidget[CardWidgetData].activeGroup = index
							cardDropped(game, display)
							heldCard = None
					}

					cardWidgets += card -> cardWidget
				}
			}

			val toRemove = cardWidgets.filterKeys(c => ! handAndAttachments.contains(c))
			for ((card, widget) <- toRemove) {
				widget.destroy()
				cardWidgets -= card
			}

			for ((card, widget) <- cardWidgets) {
				val index = effHand.indexOf(card)

				val activeDisplay = card[CardData].attachedTo.headOption.flatMap(cardWidgets.get) match {
					case Some(attachedWidget) => attachedWidget.isUnderCursor && Mouse.isInWindow && KeyboardMirror.activeModifiers.shift
					case None => heldCard.contains(card) || (heldCard.isEmpty && widget.isUnderCursor && card[CardData].attachedTo.isEmpty && Mouse.isInWindow)
				}

				widget.z = if (activeDisplay) {
					100 + (if (card[CardData].attachedTo.nonEmpty ) { -1 } else { 0 })
				} else {
					20 + (if (card[CardData].attachedTo.nonEmpty) { if (activeDisplay) { 20 } else { -20 } } else { - index })
				}


				val desiredY : Int = card[CardData].attachedTo.headOption match {
					case Some(attachedTo) =>
						cardWidgets.get(attachedTo) match {
							case Some(attachedWidget) =>
								attachedWidget.y match {
									case PositionExpression.Absolute(value, _) => value - 60
									case _ => 0
								}
							case _ => 0
						}
					case None =>
						heldCard match {
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
					card[CardData].attachedTo.headOption.flatMap(cardWidgets.get) match {
						case None => widget.parent.drawing.absolutePosition.x + 50 + xGap * index
						case Some(attachedWidget) => attachedWidget.position.x match {
							case PositionExpression.Absolute(xv,_) =>
								if (activeDisplay) {
									xv + widget.drawing.effectiveDimensions.x - 30
								} else {
									xv
								}
							case _ => 0
						}
					}
				}

				val curY = widget.position.y match {
					case PositionExpression.Absolute(value, _) => value
					case _ => desiredY
				}

				val curX = widget.position.x match {
					case PositionExpression.Absolute(value, _) => value
					case _ => desiredX
				}


				val maxSpeed = if (card[CardData].attachedTo.nonEmpty) { 64.0f } else if (heldCard.contains(card)) { 64.0f } else { 32.0f }
				val delta = (Vec2f(desiredX, desiredY) - Vec2f(curX, curY))
				val baseSpeed = if (card[CardData].attachedTo.nonEmpty) { delta.lengthSafe } else { delta.lengthSafe * 0.7f }
				val deltaN = delta.normalizeSafe * baseSpeed.max(0.0f).min(maxSpeed)

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

				widget.x = PositionExpression.Absolute(newX, TopLeft)
				widget.y = PositionExpression.Absolute(newY, TopLeft)

				val isLockedCard = DD.lockedCards.map(_.resolvedCard).contains(card)

				val od = widget[OverlayData]

				od.overlays(CardControl.ActiveOverlay).drawOverlay = Moddable(if (heldCard.contains(card) && widget.drawing.absolutePosition.y < (widget.parent.drawing.effectiveDimensions.y - 1000) && CardLogic.isPlayable(selC, card)(game.view)) {
					useCardOnDrop.set(true)
					true
				} else {
					if (heldCard.contains(card)) {
						useCardOnDrop.set(false)
					}
					false
				})

				od.overlays(CardControl.LockedOverlay).drawOverlay = Moddable(isLockedCard)
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

		if (useCardOnDrop.get()) {
			for (card <- heldCard ; selC <- tuid.selectedCharacter) {
				if (CardLogic.isPlayable(selC, card)) {
					val cardPlay = CardPlay(selC, card, heldEffectGroup)
					cardPlay.instantiate(game.view, selC, card) match {
						case Left(cardPlayInst) =>
							selectedCard = Some(card)
							selectionControl.changeSelectionTarget(game, display, cardPlay, cardPlayInst,
								(sc) => {
									CardLogic.playCard(selC, card, cardPlayInst, sc.selectionResults)(game)
									selectedCard = None
								},
								() => selectedCard = None)
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

case class CardInfo(name : String, tags : List[Taxon], image : Image, mainCost : RichText, secondaryCost : RichText, effects : Vector[RichText]) {
	val hasTags = tags.nonEmpty
}
object CardInfo {
	import arx.Prelude._

	def apply(character : Entity, card : Entity, activeEffectGroup : Int)(implicit view : WorldView) : CardInfo = {
		CardInfo(Some(character), card(IdentityData).kind, card(CardData), card(TagData), activeEffectGroup)
	}
	def apply(character : Option[Entity], cardKind : Taxon, CD : CardData, TD : TagData, activeEffectGroupIndex : Int)(implicit view : WorldView) : CardInfo = {
		val settings = RichTextRenderSettings()

		val costAndEffectSections = for (CostsAndEffects(costs, effects, selfEffects, triggeredEffects, description) <- CardLogic.effectiveCostsAndEffects(character, CD)) yield {
			val apCosts = costs.collect {
				case PayActionPoints(ap) => ap
			}
			val staminaCosts = costs.collect {
				case PayStamina(stamina) => stamina
			}

			val mainCost = if (apCosts.nonEmpty) {
				val totalCost = apCosts.sum
				RichText(TextSection(totalCost.toString) :: TaxonSections("GameConcepts.ActionPoint", settings))
			} else {
				RichText.Empty
			}

			val secondaryCost = if (staminaCosts.nonEmpty) {
				val totalCost = staminaCosts.sum + TagLogic.flagValue(TD, Taxonomy("StaminaCostDelta"))
				RichText(TextSection(totalCost.toString) :: TaxonSections("GameConcepts.StaminaPoint", settings))
			} else {
				RichText.Empty
			}

			val effectTextSections = effects.map(e => {
				character match {
					case Some(c) => e.toRichText(view, c, RichTextRenderSettings()).sections
					case None => e.toRichText(settings).sections
				}
			})
			val selfEffectTextSections = selfEffects.map(e => e.toRichText(settings).sections)

			val combinedEffectsSections = description match {
				case Some(desc) => RichText.parse(desc, settings).sections
				case None => (effectTextSections ++ selfEffectTextSections).reduceLeftOrElse((t1, t2) => t1 ++ Seq(LineBreakSection(0)) ++ t2, Vector())
			}

			val triggeredEffectSection = triggeredEffects.map(LineBreakSection(0) + _.toRichText(settings)).foldLeft(RichText.Empty)(_ ++ _)

			val effectText = RichText(combinedEffectsSections) ++ triggeredEffectSection
			(mainCost, secondaryCost, effectText)
		}

		var attachmentSections = Vector[RichTextSection]()

		for ((attachKey, attachment) <- CD.attachments) {
			if (! CD.attachedCards.contains(attachKey)) {
				attachment.attachmentStyle match {
					case AttachmentStyle.Contained =>
					case AttachmentStyle.PlayModified(effectModifiers) =>
						val conditionSections : Vector[RichTextSection] = attachment.condition.flatMap(_.toRichText(settings).sections)
						attachmentSections ++= TaxonSections("Attach", settings)
						attachmentSections ++= conditionSections
						attachmentSections :+= LineBreakSection(0)

						attachmentSections ++= effectModifiers.map(e => e.toRichText(settings).sections)
							.reduceLeftOrElse((t1, t2) => t1 ++ Seq(LineBreakSection(0)) ++ t2, Vector())
				}
			}
		}

		val (mainCost, secondaryCost, _) = if (costAndEffectSections.size > activeEffectGroupIndex) { costAndEffectSections(activeEffectGroupIndex) } else { costAndEffectSections.head }

		var effectText = Vector[RichText]()
		for (((_,_,text),index) <- costAndEffectSections.zipWithIndex) {

			if (index == activeEffectGroupIndex) {
				effectText ++= text
			} else {
				effectText ++= RichText(text.sections.map(_.applyTint(Moddable(RGBA(1.0f,1.0f,1.0f,1.0f)))))
			}
		}

		var tagsSections = TD.tags.toVector.map(t => TagLibrary.withKind(t))
			.filter(!_.hidden)
   		.map(_.toRichText(settings))
   		.reduceLeftOrElse((t1, t2) => t1 + TextSection(", ") ++ t2, RichText(Vector()))
		if (!tagsSections.isEmpty) {
			tagsSections = tagsSections + LineBreakSection(20)
		}

		val name = CD.cardEffectGroups(activeEffectGroupIndex).name.getOrElse(CD.name)

		val cardImage : TToImage = CardImageLibrary.cardImageOpt(name) match {
			case Some(img) => img
			case _ =>
				if (cardKind.isA(CardTypes.ItemCard)) {
					"graphics/card_images/item_background.png"
				} else if (cardKind.isA(CardTypes.AttackCard)) {
					"graphics/card_images/punch.png"
				} else if (cardKind.isA(CardTypes.MoveCard)) {
					"graphics/card_images/move.png"
				} else if (cardKind.isA(CardTypes.StatusCard)) {
					"graphics/card_images/status_background.png"
				} else {
					"default/blank_transparent.png"
				}
		}

		CardInfo(name.capitalize, TD.tags.toList, cardImage, mainCost, secondaryCost, effectText)
	}
}

