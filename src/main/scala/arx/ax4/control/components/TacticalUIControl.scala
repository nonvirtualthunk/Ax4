package arx.ax4.control.components

import arx.ai.search.Pathfinder
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical, Tile, TurnData}
import arx.ax4.game.entities.{AllegianceData, CharacterInfo, DamageElement, FactionData, Physical, Tile, Tiles, TurnData}
import arx.ax4.game.event.{ActiveIntentChanged, EntityMoved}
import arx.ax4.graphics.components.EntityGraphics
import arx.ax4.graphics.data.{AxDrawingConstants, TacticalUIData}
import arx.core.units.UnitOfTime
import arx.core.vec.{ReadVec2f, Vec3f}
import arx.core.vec.coordinates.{AxialVec, AxialVec3}
import arx.engine.control.components.ControlComponent
import arx.engine.control.event.{KeyModifiers, KeyPressEvent, KeyReleaseEvent, MouseButton, MouseMoveEvent, MousePressEvent, UIEvent}
import arx.engine.graphics.data.PovData
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World, WorldView}
import arx.graphics.GL
import arx.Prelude.toArxList
import arx.ax4.game.action.{AttackIntent, BiasedAxialVec3, DoNothingIntent, GameAction, MoveIntent}
import arx.ax4.game.event.TurnEvents.{TurnEndedEvent, TurnStartedEvent}
import arx.ax4.game.logic.{Action, CombatLogic}
import arx.core.datastructures.{Watcher, Watcher2}
import arx.core.math.Sext
import arx.engine.control.components.windowing.WindowingControlComponent
import arx.engine.control.data.WindowingControlData
import arx.engine.data.Moddable
import arx.engine.entity.{Entity, IdentityData}
import arx.resource.ResourceManager
import org.lwjgl.glfw.GLFW

class TacticalUIControl(windowing : WindowingControlComponent) extends AxControlComponent {
	import arx.core.introspection.FieldOperations._

	var lastSelected : (Option[Entity], GameEventClock) = (None, GameEventClock(0))


	override def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val view = gameView
		val tuid = display[TacticalUIData]
		val selectedState = (tuid.selectedCharacter, view.currentTime)
		if (selectedState != lastSelected) {
			updateBindings(view, display, tuid.selectedCharacter)
			lastSelected = selectedState
		}
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = game.view

		val tuid = display[TacticalUIData]

		val desktop = display[WindowingControlData].desktop
		desktop.drawing.drawAsForegroundBorder = true
		desktop.drawing.backgroundImage = Some(ResourceManager.image("ui/woodBorder2.png"))
		val selCInfo = desktop.createChild("widgets/CharacterInfoWidgets.sml", "SelectedCharacterInfo")
		selCInfo.showing = Moddable(() => tuid.selectedCharacter.isDefined)
		tuid.selectedCharacterInfoWidget = selCInfo


		val activeFaction = gameView.worldData[TurnData].activeFaction
		for (selectChar <- gameView.entitiesWithData[CharacterInfo]
			.filter(e => e[AllegianceData].faction == activeFaction)
			.find(e => e[CharacterInfo].actionPoints.currentValue > 0 || e[CharacterInfo].movePoints > Sext(0))) {
			selectCharcter(game, display, selectChar)
		}


		onControlEvent {
			case MousePressEvent(button, pos, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val pressedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				fireEvent(HexMousePressEvent(button,pressedHex,pos,modifiers))
			case MouseMoveEvent(pos, delta, modifiers) =>
				val unprojected = display[PovData].pov.unproject(Vec3f(pos,0.0f), GL.viewport)
				val const = display[AxDrawingConstants]
				val mousedHex = AxialVec.fromCartesian(unprojected.xy, const.HexSize)

				val tuid = display[TacticalUIData]
				val biasDir = (0 until 6).minBy(q => (mousedHex.neighbor(q).asCartesian(const.HexSize.toFloat) - unprojected.xy).lengthSafe)
				tuid.mousedOverBiasedHex = BiasedAxialVec3(AxialVec3(mousedHex,0), biasDir)


				fireEvent(HexMouseMoveEvent(mousedHex,pos,modifiers))
			case KeyReleaseEvent(GLFW.GLFW_KEY_ENTER, _) =>
				println("Ending turn")
				val TD = game.view.worldData[TurnData]
				val factions = game.view.entitiesWithData[FactionData].toList.sortBy(e => e.id)
				val activeFaction = TD.activeFaction
				val activeIndex = factions.indexOf(activeFaction)
				val nextFaction = factions((activeIndex + 1) % factions.size)
				game.addEvent(TurnEndedEvent(activeFaction, TD.turn))

				if (activeIndex == factions.size - 1) {
					game.modifyWorld(TurnData.turn + 1)
				}

				game.modifyWorld(TurnData.activeFaction -> nextFaction)
				game.addEvent(TurnStartedEvent(nextFaction, TD.turn))
			case HexMousePressEvent(button, hex, pos, modifiers) =>
				val tuid = display[TacticalUIData]
				for (sel <- tuid.consideringSelection) {
					selectCharcter(game, display, sel)
				}

				tuid.consideringActionSelectionContext match {
					case Some(ActionSelectionContext(intent, selectionResults)) =>
						if (! intent.hasRemainingSelections(selectionResults)) {
							for (action <- intent.createAction(selectionResults.build())) {
								Action.performAction(action)(game)
							}
						} else {
							// lock in the considered value here when we press
							tuid.actionSelectionContext = Some(ActionSelectionContext(intent, selectionResults))
						}
					case None =>
				}
		}
	}

	def selectCharcter(game : World, display : World, selC: Entity) = {
		implicit val view = game.view
		val tuid = display[TacticalUIData]
		tuid.selectedCharacter = Some(selC)
		if (selC.data[CharacterInfo].activeIntent == DoNothingIntent) {
			val availableAttacks = CombatLogic.availableAttacks(game.view, selC)
			val newIntent = availableAttacks.find(a => a.attackKey == "primary")
   			.orElse(availableAttacks.headOption) match {
				case Some(attackRef) => AttackIntent(attackRef)
				case None => MoveIntent
			}
			println(s"New Intent: $newIntent")

			game.startEvent(ActiveIntentChanged(selC, newIntent))
			game.modify(selC, CharacterInfo.activeIntent -> newIntent)
			game.endEvent(ActiveIntentChanged(selC, newIntent))
		}
	}

	def updateBindings(implicit game : WorldView, display : World, character : Option[Entity]): Unit = {
		for (selC <- display[TacticalUIData].selectedCharacter) {
			val desktop = display[WindowingControlData].desktop
			desktop.bind("selectedCharacter.name", selC[IdentityData].name.getOrElse("nameless"))
			desktop.bind("selectedCharacter.portrait", () => EntityGraphics.characterImageset.drawInfoFor(game, selC, display.view).head.image)

			desktop.bind("selectedCharacter.health.cur", () => selC[CharacterInfo].health.currentValue)
			desktop.bind("selectedCharacter.health.max", () => selC[CharacterInfo].health.maxValue)

			desktop.bind("selectedCharacter.actions.cur", () => selC[CharacterInfo].actionPoints.currentValue)
			desktop.bind("selectedCharacter.actions.max", () => selC[CharacterInfo].actionPoints.maxValue)

			desktop.bind("selectedCharacter.move.cur", () => selC[CharacterInfo].curPossibleMovePoints)
			desktop.bind("selectedCharacter.move.max", () => selC[CharacterInfo].maxPossibleMovePoints)

			desktop.bind("selectedCharacter.speed", () => selC[CharacterInfo].moveSpeed)

			val selCWidget = display[TacticalUIData].selectedCharacterInfoWidget
			selCWidget.bind("attacks", () => {
				CombatLogic.availableAttacks(game, selC)
   				.sortBy(aref => aref.attackKey != "primary")
					.map(attack => {
					CombatLogic.resolveUntargetedConditionalAttackData(game, selC, attack) match {
						case Some((attackData, modifiers)) =>
							SimpleAttackDisplayInfo(attackData.name.capitalize, AttackBonus(attackData.accuracyBonus), DamageExpression(attackData.damage))
						case None => SimpleAttackDisplayInfo("unresolved", AttackBonus(0), DamageExpression(Map()))
					}
				}).toList
			})
		}
	}
}

case class DamageExpression(damageElements : Map[AnyRef, DamageElement]) {
	import arx.Prelude.int2RicherInt
	override def toString: String = {
		damageElements.values.map(de => {
			var sections : List[String] = Nil
			sections ::= de.damageDice.toString
			if (de.damageMultiplier != 1.0f) {
				sections ::= s"x${de.damageMultiplier}"
			}
			if (de.damageBonus != 0) {
				sections ::= de.damageBonus.toSignedString
			}
			sections ::= de.damageType.name
			sections.reverse.reduce(_ + " " + _)
		}).foldLeft("")(_ + " " + _).trim
	}
}
case class AttackBonus(bonus : Int) {
	import arx.Prelude.int2RicherInt
	override def toString: String = bonus.toSignedString
}
case class SimpleAttackDisplayInfo(name : String, accuracyBonus : AttackBonus, damage : DamageExpression)

case class HexMousePressEvent(button : MouseButton, hex : AxialVec, pos : ReadVec2f, modifiers : KeyModifiers) extends UIEvent
case class HexMouseMoveEvent(hex : AxialVec, pos : ReadVec2f, modifiers: KeyModifiers) extends UIEvent