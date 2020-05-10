package arx.ax4.control.components
import java.util.concurrent.atomic.AtomicBoolean

import arx.Prelude
import arx.application.Noto
import arx.ax4.control.components.widgets.{CardWidget, CardWidgetData}
import arx.ax4.game.entities.Companions.{CardData, DeckData, TagData}
import arx.ax4.game.entities.cardeffects.{PayActionPoints, PayStamina}
import arx.ax4.game.entities.{AttachmentStyle, CardData, CardPlay, CardTypes, TagData, TagLibrary}
import arx.ax4.game.logic.{AllegianceLogic, CardLocation, CardLogic, TagLogic}
import arx.ax4.game.logic.CardLogic.CostsAndEffects
import arx.ax4.graphics.data.{CardImageLibrary, TacticalUIData}
import arx.core.math.Rectf
import arx.core.metrics.Metrics
import arx.core.vec.{Cardinals, ReadVec3f, ReadVec3i, Vec2T, Vec3f, Vec3i}
import arx.engine.control.components.windowing.widgets.{BottomLeft, BottomRight}
import arx.engine.control.components.windowing.widgets.data.TWidgetAuxData
import arx.engine.world.GameEventClock
import overlock.atomicmap.AtomicMap
//import arx.core.datastructures.OneOrMore.fromSingle
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2i, Vec2f, Vec2i}
import arx.engine.control.components.windowing.{Widget, WidgetData}
import arx.engine.control.components.windowing.widgets.data.{DrawingData, OverlayData, WidgetOverlay}
import arx.engine.control.components.windowing.widgets.{ListItemMousedOver, ListItemSelected, PositionExpression, TopLeft}
import arx.engine.control.event.{KeyModifiers, KeyboardMirror, Mouse, MouseButton, MousePressEvent, MouseReleaseEvent}
import arx.engine.data.Moddable
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.graphics.helpers._
import arx.graphics.{Image, TToImage}
import arx.resource.ResourceManager


case class CardPositioning (currentPosition : ReadVec2i, desiredPosition : ReadVec2i, lastUpdate : UnitOfTime)

class CardControl(selectionControl : SelectionControl) extends AxControlComponent {

	var cardWidgets : Map[Entity, Widget] = Map()

	val unselectedDepth = -450
	val selectedDepth = 20

	var pileWidgets : Map[CardLocation, Widget] = Map()

	def updateCards(tuid : TacticalUIData, game : World, display : World)(implicit view : WorldView) : Map[Entity, Widget] = {
		tuid.selectedCharacter match {
			case Some(selC) if AllegianceLogic.isPlayerCharacter(selC) =>
				val DD = selC(DeckData)

				val handWithoutSelected = DD.hand.filterNot(h => cardWidgets.get(h).exists(w => w[CardInHand].isSelected))
				val handAndAttachments = DD.hand.flatMap(c => CardLogic.cardAndAllAttachments(c))

				val handRect = calculateHandRect

				val res = for (card <- handAndAttachments) yield {
					card -> cardWidgets.getOrElse(card, createCardWidget(tuid.fullGameAreaWidget, selC, card))
				}
				val newWidgets = res.toMap

				// destory old widgets that are no longer necessary
				val existingWidgets = cardWidgets
				existingWidgets.filterKeys(e => !newWidgets.contains(e)).values.foreach(w => w.destroy())

				// mark everything as unheld if the mouse is not down
				if (!Mouse.buttonDown.getOrElse(MouseButton.Left, false)) {
					newWidgets.foreach(_._2[CardInHand].held = false)
				}

				// interpolate positions and update overlays
				val heldCard = newWidgets.find(_._2[CardInHand].held).map(_._1)
				for ((card,widget) <- newWidgets) {
					val cardInHand = widget[CardInHand]
					widget.showing = Moddable(!cardInHand.isSelected)
					val desired = Vec3f(desiredPositionFor(card, widget, newWidgets, heldCard, handWithoutSelected.indexOf(card), handRect))
					card[CardData].attachedTo.headOption.flatMap(newWidgets.get) match {
						case Some(otherWidget) =>
							widget.x = PositionExpression.Relative(otherWidget, 50, Cardinals.Right)
							widget.y = PositionExpression.Relative(otherWidget, 50, Cardinals.Up)
							widget.z = desired.z.round
						case None =>
							val dt = Prelude.curTime() - cardInHand.updatedAt
							cardInHand.updatedAt = Prelude.curTime()
							val eff = moveTo(cardInHand.position, desired, (if (heldCard.contains(card)) { 2000.0f } else { cardInHand.velocity }) * (dt.inSeconds / 0.0166667f))
							if (eff != desired) {
								cardInHand.velocity += 2f
							} else {
								cardInHand.velocity = 32.0f
							}
							cardInHand.position = eff
							widget.x = PositionExpression.Absolute(eff.x.round)
							widget.y = PositionExpression.Absolute(eff.y.round)
							widget.z = eff.z.round
					}

					val isLockedCard = DD.lockedCards.map(_.resolvedCard).contains(card)

					val od = widget[OverlayData]

					val playable = heldCard.contains(card)

					cardInHand.useCardOnDrop = widget.drawing.absolutePosition.y < (widget.parent.drawing.effectiveDimensions.y - 1000) && CardLogic.isPlayable(selC, card)
					od.overlays(CardControl.ActiveOverlay).drawOverlay = Moddable(playable)
					od.overlays(CardControl.LockedOverlay).drawOverlay = Moddable(isLockedCard)
				}

				Mouse.setVisible(heldCard.isEmpty)

				newWidgets
			case None =>
				Map()
		}
	}

	def calculateHandRect = {
		val drawPileDraw = pileWidgets(CardLocation.DrawPile).drawing
		val discardPileDraw = pileWidgets(CardLocation.DiscardPile).drawing
		Rectf(drawPileDraw.absolutePosition.x + drawPileDraw.effectiveDimensions.x + 100, 0, discardPileDraw.absolutePosition.x - 100, 500)
	}

	def createCardWidget(parentWidget : Widget, selC : Entity, card : Entity)(implicit view : WorldView) = {
		val cardWidget = CardWidget(parentWidget, selC, card)
		val posY = cardWidget.parent.drawing.absolutePosition.y + cardWidget.parent.drawing.effectiveDimensions.y + unselectedDepth
		cardWidget.y = PositionExpression.Absolute(posY, TopLeft)

		cardWidget.attachData[CardInHand]
		cardWidget[CardInHand].position = Vec3i(0, posY, 0)
		cardWidget[CardInHand].desiredPosition = Vec3i(0, posY, 0)

		cardWidget.onEvent {
			case MousePressEvent(button, pos, modifiers) =>
				cardWidget[CardInHand].held = true
				cardWidget[CardInHand].grabOffset = cardWidget.windowingSystem.currentWindowingMousePosition - cardWidget.drawing.absolutePosition.xy
			case MouseReleaseEvent(button, pos, modifiers) =>
				cardWidget[CardInHand].dropTriggered = true
				cardWidget[CardInHand].held = false
			case ListItemMousedOver(_, index, _) =>
				cardWidget[CardInHand].activeEffectGroup = index
				cardWidget[CardWidgetData].activeGroup = index
			case ListItemSelected(_, index, _) =>
				cardWidget[CardInHand].activeEffectGroup = index
				cardWidget[CardWidgetData].activeGroup = index
				cardWidget[CardInHand].dropTriggered = true
				cardWidget[CardInHand].held = false
		}
		cardWidget
	}

	def desiredPositionFor(card : Entity, widget : Widget, widgets : Map[Entity,Widget], heldCard : Option[Entity], index : Int, handRect : Rectf)(implicit view : WorldView) = {
		val availableSpace = handRect.width
		val xGap = (availableSpace / (widgets.size-1).max(1)).min((widget.drawing.effectiveDimensions.x * 0.75f).toInt)

		val desiredX = if (heldCard.contains(card)) {
			widget.windowingSystem.currentWindowingMousePosition.x - widget[CardInHand].grabOffset.x
		} else {
			handRect.x + 50 + xGap * index
		}

		val desiredY: Int = heldCard match {
			case Some(held) if card == held =>
				widget.windowingSystem.currentWindowingMousePosition.y - widget[CardInHand].grabOffset.y
			case None if widget.isUnderCursor =>
				widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y - widget.drawing.effectiveDimensions.y + selectedDepth
			case _ =>
				widget.parent.drawing.absolutePosition.y + widget.parent.drawing.effectiveDimensions.y + unselectedDepth
		}

		val desiredZ = 20 + (if (card[CardData].attachedTo.nonEmpty) { -20 } else { - index })

		Vec3i(desiredX.toInt, desiredY.toInt, desiredZ.toInt)
	}

	def moveTo(a : ReadVec3f, t : ReadVec3f, maxVelocity : Float) = {
		val d = t - a
		val n = d.normalizeSafe
		val l = d.lengthSafe
		if (maxVelocity > l) {
			t
		} else {
			a + n * l.min(maxVelocity)
		}
	}

	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = gameView

		cardWidgets = updateCards(tuid, game, display)

		for ((card,widget) <- cardWidgets) {
			val cardInHand = widget[CardInHand]
			if (cardInHand.dropTriggered) {
				cardDropped(card, widget, game, display)
				cardInHand.dropTriggered = false
			}
		}
	}

	override protected def onInitialize(hypView: HypotheticalWorldView, game: World, display: World): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = game.view

		pileWidgets = List(CardLocation.DrawPile, CardLocation.DiscardPile)
			.map(l => l -> PileWidget.create(tuid, l)(hypView))
			.toMap
	}

	def cardDropped(card : Entity, widget : Widget, game : World, display : World): Unit = {
		val tuid = display[TacticalUIData]
		implicit val view = game.view

		for (selC <- tuid.selectedCharacter) {
			val cardInHand = widget[CardInHand]
			if (cardInHand.useCardOnDrop) {
				if (CardLogic.isPlayable(selC, card)) {
					val cardPlay = CardPlay(selC, card, cardInHand.activeEffectGroup)
					cardPlay.instantiate(game.view, selC, card) match {
						case Left(cardPlayInst) =>
							cardInHand.isSelected = true
							selectionControl.changeSelectionTarget(game, display, cardPlay, cardPlayInst,
								(sc) => {
									CardLogic.playCard(selC, card, cardPlayInst, sc.selectionResults)(game)
								},
								() => {})
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

class CardInHand extends TWidgetAuxData {
	var activeEffectGroup: Int = 0
	var isSelected: Boolean = false
	var grabOffset: ReadVec2i = Vec2i.Zero
	var dropTriggered: Boolean = false
	var held : Boolean = false
	var useCardOnDrop: Boolean = false
	var position: ReadVec3f = Vec3f.Zero
	var desiredPosition : ReadVec3f = Vec3f(0,0,0)
	var updatedAt: UnitOfTime = Prelude.curTime()
	var velocity: Float = 10.0f
}
object CardInHand {
	val Sentinel = new CardInHand
}

object PileWidget {
	val iconsByLocation : Map[CardLocation, Image] = Map[CardLocation, String](
		CardLocation.Hand -> "graphics/icons/card_back_large.png",
		CardLocation.DrawPile -> "graphics/icons/card_back_large.png",
		CardLocation.DiscardPile -> "graphics/icons/card_back_large.png",
		CardLocation.ExhaustPile -> "graphics/icons/card_back_large.png",
		CardLocation.ExpendedPile -> "graphics/icons/card_back_large.png",
		CardLocation.NotInDeck -> "graphics/icons/card_back_large.png"
	).mapValues(s => ResourceManager.image(s))

	case class PileInfo(icon : Image, cardCount : Int, showing : Boolean)
	def create(tuid : TacticalUIData, location : CardLocation)(implicit view : WorldView) = {
		val pileWidget = tuid.fullGameAreaWidget.createChild("CardWidgets.CardPileWidget")
		location match {
			case CardLocation.Hand => Noto.warn("displaying hand as a pile doesn't make sense")
			case CardLocation.DrawPile => pileWidget.position = Vec2T(PositionExpression.Constant(10, BottomLeft), PositionExpression.Constant(10, BottomLeft))
			case CardLocation.DiscardPile => pileWidget.position = Vec2T(PositionExpression.Constant(10, BottomRight), PositionExpression.Constant(10, BottomRight))
			case CardLocation.ExhaustPile => Noto.warn("displaying exhaust pile not yet supported")
			case CardLocation.ExpendedPile => Noto.warn("displaying expended pile not yet supported")
			case CardLocation.NotInDeck => Noto.warn("displaying not-in-deck pile doesn't make sense")
		}

		pileWidget.bind("pile", () => tuid.selectedCharacter match {
			case Some(selC) => PileInfo(iconsByLocation(location), CardLogic.cardsInLocation(selC, location).size, showing = true)
			case None => PileInfo(iconsByLocation(location), 0, showing = false)
		})

		pileWidget
	}
}

case class CardInfo(name : String, tags : List[Taxon], image : Image, mainCost : RichText, secondaryCost : RichText, effects : Vector[RichText]) {
	val hasTags = tags.nonEmpty
}
object CardInfo {
	import arx.Prelude._

	val cacheHits = Metrics.counter("CardInfo.cacheHits")
	val totalCreations = Metrics.counter("CardInfo.creations")

	case class CardInfoKey(entity : Entity, activeGroup : Int, cardKind : Taxon)
	// TODO: Clear this out, occassionally
	val cardInfoCache = AtomicMap.atomicNBHM[CardInfoKey, (GameEventClock, CardInfo)]

	def apply(character : Entity, card : Entity, activeEffectGroup : Int)(implicit view : WorldView) : CardInfo = {
		CardInfo(card, Some(character), card(IdentityData).kind, card(CardData), card(TagData), activeEffectGroup)
	}
	def apply(cardEntity : Entity, character : Option[Entity], cardKind : Taxon, CD : CardData, TD : TagData, activeEffectGroupIndex : Int)(implicit view : WorldView) : CardInfo = {
		totalCreations.inc()
		val cacheKey = CardInfoKey(cardEntity, activeEffectGroupIndex, cardKind)
		val cached = cardInfoCache.get(cacheKey)
		if (cached.isDefined) {
			if (cached.get._1 == view.currentTime) {
				cacheHits.inc()
				return cached.get._2
			}
		}

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
				effectText :+= text
			} else {
				effectText :+= RichText(text.sections.map(_.applyTint(Moddable(RGBA(1.0f,1.0f,1.0f,1.0f)))))
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

		val info = CardInfo(name.capitalize, TD.tags.toList, cardImage, mainCost, secondaryCost, effectText)
		cardInfoCache.put(cacheKey, (view.currentTime, info))
		info
	}
}

