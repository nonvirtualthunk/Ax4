package arx.ax4.control.components
import arx.application.Noto
import arx.ax4.control.components.widgets.CardWidget
import arx.ax4.game.entities.AttackConditionals.AnyAttack
import arx.ax4.game.entities.cardeffects.{AddAttackModifierEffect, AddCardToDeck, GameEffect}
import arx.ax4.game.entities.{CharacterInfo, PendingPerkPicks, Perk, PerkSource, PerksLibrary}
import arx.ax4.game.event.NewPerkPicksAvailable
import arx.ax4.game.logic.PerkLogic
import arx.ax4.graphics.data.TacticalUIData
import arx.core.geometry.{Horizontal, Vertical}
import arx.core.units.UnitOfTime
import arx.engine.control.components.windowing.{Div, SimpleWidget, Widget}
import arx.engine.control.components.windowing.widgets.{Center, DynamicWidgetData, DynamicWidgetFunctions, ListWidgetDynamicFunctions, PositionExpression, TextDisplayWidget}
import arx.engine.control.data.WindowingControlData
import arx.engine.control.event.MouseReleaseEvent
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.graphics.helpers.{LineBreakSection, RichText, RichTextRenderSettings, RichTextSection}

class PerkPickControl(tacticalUIControl: TacticalUIControl) extends AxControlComponent {
	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(view: HypotheticalWorldView, game: World, display: World): Unit = {
//		onGameEventEnd {
//			case NewPerkPicksAvailable()
//		}
		implicit val implView = view

		val tuid = display[TacticalUIData]
		tuid.perkSelectionWidget = tuid.mainSectionWidget.createChild("PerkSelectionWidgets.PerkSelectionWidget")
		tuid.perkSelectionWidget.showing = Moddable(() => activePerkSel(tuid).nonEmpty)
		tuid.perkSelectionWidget.bind("perkSelection.source", () => activePerkSel(tuid).map(_.source))

		val perkOptions = tuid.perkSelectionWidget.descendantsWithIdentifier("PerkOptions").head
		perkOptions.data[DynamicWidgetData].dynWidgetFunctions = new PerkSelectionDynamicFunctions(tuid, view, game)
	}


	def activePerkSel(tuid : TacticalUIData)(implicit view : WorldView) : Option[PendingPerkPicks] = {
		tuid.selectedCharacter.flatMap(c => view.data[CharacterInfo](c).pendingPerkPicks.headOption)
	}
}


class PerkSelectionDynamicFunctions(tuid : TacticalUIData, implicit val view : WorldView, implicit val world : World) extends DynamicWidgetFunctions {
	override def computeChildrenData(dynWidget: Widget): List[Any] = {
		tuid.selectedCharacter.flatMap(_.data[CharacterInfo].pendingPerkPicks.headOption)
			.map(p => p.possiblePerks.map(p.source -> _)).getOrElse(Vector())
			.toList
	}

	override def createChildFromData(dynWidget: Widget, data: Any): Widget = {
		data match {
			case (source : PerkSource, perkTaxon : Taxon) =>
				PerksLibrary.getWithKind(perkTaxon) match {
					case Some(perk) =>
						val div = dynWidget.createChild("PerkSelectionWidgets.PerkInfoWidget")
						div.bind("perkInfo", PerkInfo(perk, perk.icon.isDefined, Moddable(() => div.isUnderCursor)))
						val effectsDiv = div.descendantsWithIdentifier("PerkEffects").head
						// TODO: Actually support multiple effects in one perk, we realllllly don't right now
						val perkWidgets = perk.effects.map(effect => {
							GameEffectWidget.widgetForEffect(effect, effectsDiv)
						})

						div.onEvent {
							case MouseReleaseEvent(button, pos, modifiers) =>
								PerkLogic.takePerk(tuid.selectedCharacter.getOrElse(Entity.Sentinel), perkTaxon, source)
						}

						div
					case None =>
						Noto.warn(s"Perk with no info: $perkTaxon")
						dynWidget.createChild(SimpleWidget).widget

				}
			case other =>
				Noto.warn(s"Non-taxon in perk selection, $other")
				dynWidget.createChild(SimpleWidget).widget
		}
	}

	override def arrangeChildren(dynWidget: Widget, childWidgets: List[Widget]): Unit = {
		childWidgets.size match {
			case 0 => // do nothing
			case 1 => childWidgets.head.x = PositionExpression.Centered
			case _ =>
				// TODO : deal with case in which naive placement puts the first/last outside of bounds
				val increment = 1.0f / childWidgets.size.toFloat

				childWidgets.zipWithIndex.foreach {
					case (child, index) =>
						val fract = (index.toFloat + 0.5f) * increment
						child.widgetData.x = PositionExpression.Proportional(fract, anchorTo=Center)
				}
		}
	}
}

case class PerkInfo (perk : Perk, hasIcon : Boolean, selected : Moddable[Boolean])


object GameEffectWidget {
	def widgetForEffect(effect : GameEffect, parent : Widget)(implicit view : WorldView) : Widget = {
		effect match {
			case AddCardToDeck(cardArchetypes, cardAdditionStyle) =>
				val div = parent.createChild(Div)
				val cardWidgets = cardArchetypes.map(arch => {
					CardWidget(div, None, arch)
				})
				div
			case AddAttackModifierEffect(condition, modifier) =>
				val settings = RichTextRenderSettings()
				var res = RichText(Nil)
				if (condition != AnyAttack) {
					res = res.append("When ").append(condition.toRichText(settings)).append(LineBreakSection(0))
				}
				res = res.append(modifier.toRichText(settings))

				val td = parent.createChild(TextDisplayWidget)
				td.text = Moddable(res)
				td.drawBackground = false
				td.fontScale = 1.5f
				td
			case _ =>
				Noto.warn("Unsupported game effect to turn into a widget")
				val td = parent.createChild(TextDisplayWidget)
				td.text = Moddable(RichText(effect.toString))
				td
		}
	}
}