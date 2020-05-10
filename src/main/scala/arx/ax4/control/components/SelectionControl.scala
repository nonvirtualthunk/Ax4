package arx.ax4.control.components
import arx.ax4.game.action.{MoveCharacter, Selectable, SelectableInstance, SelectionResult, Selector}
import arx.ax4.game.entities.{AllegianceData, FactionData, Tiles, TurnData}
import arx.ax4.graphics.data.TacticalUIData
import arx.core.units.UnitOfTime
import arx.engine.control.components.windowing.Widget
import arx.engine.control.data.{TControlData, WindowingControlData}
import arx.engine.entity.Entity
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.Prelude._
import arx.application.Noto
import arx.ax4.control.event.SelectionMadeEvent
import arx.ax4.game.logic.AllegianceLogic
import arx.ax4.graphics.components.subcomponents.TacticalSelectableRenderer
import arx.core.introspection.ReflectionAssistant
import arx.core.metrics.Metrics
import arx.engine.control.event.{KeyModifiers, KeyReleaseEvent}
import arx.engine.data.{TMutableAuxData, TWorldAuxData}
import org.lwjgl.glfw.GLFW

class SelectionControl(mainControl : TacticalUIControl) extends AxControlComponent {
	lazy val selectableRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalSelectableRenderer]

	var ticker = 0

	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val SD = display[SelectionData]

		for (selC <- display[TacticalUIData].selectedCharacter) {
			if (SD.activeContext.isEmpty || SD.activeContext.get.entity != selC) {
				val inst = MoveCharacter.forceInstantiate(game.view, selC)
				SD.activeContext = Some(SelectionContext(selC, MoveCharacter, inst, SelectionResult(), sc => inst.applyEffect(game, sc.selectionResults), () => {}))
			}
		}

		SD.consideredContext = selectionContextIfClicked(game.view, display)

		val desktop = display[WindowingControlData].desktop
		for (selC <- display[TacticalUIData].selectedCharacter; consCtxt <- SD.consideredContext; actCtxt <- SD.activeContext) {
			selectableRenderers.foreach(r => r.updateUI(gameView, display, selC, consCtxt.selectable, consCtxt.selectableInst, consCtxt.selectionResults, actCtxt.selectionResults, desktop))
			selectableRenderers.foreach(r => r.update(gameView, display, selC, consCtxt.selectable, consCtxt.selectableInst, consCtxt.selectionResults))
		}
	}

	override protected def onInitialize(view: HypotheticalWorldView, game: World, display: World): Unit = {
		val SD = display[SelectionData]
		implicit val view = game.view

		display[TacticalUIData].mainSectionWidget.onEvent {
			case SelectionMadeEvent(selector, value, amount) =>
				updateSelectionContext(view, display, SD.activeContext.map(ctxt => ctxt.copy(selectionResults = ctxt.selectionResults.addResult(selector, value, amount))))
		}

		onControlEvent {
			case HexMouseReleaseEvent(button, hex, pos, modifiers) =>
				val newCtxt = selectionContextIfClicked(view, display)
				if (newCtxt != SD.activeContext) {
					updateSelectionContext(view, display, newCtxt)
				} else {
					val tuid = display[TacticalUIData]
					val activeFaction = view.worldData[TurnData].activeFaction
					for (mousedEnt <- Tiles.entitiesOnTile(tuid.mousedOverHex)) {
						if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction) && activeFaction[FactionData].playerControlled) {
							mainControl.selectCharacter(game, display, mousedEnt)
						}
					}
				}
			case KeyReleaseEvent(GLFW.GLFW_KEY_ESCAPE, KeyModifiers.None) =>
				updateSelectionContext(view, display, None)
		}
	}


	var lastSelectionContextIfClicked : Option[SelectionContext] = None
	var lastSelectionContextIfClickedKey : Option[(List[Any], Entity, Selectable, Selector[_])] = None
	val skippedRecalculationCounter = Metrics.counter("SelectionControl.selectionContextIfClickedShortCircuit")

	def selectionContextIfClicked(implicit view : WorldView, display : World) : Option[SelectionContext] = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]

		val toConsiderForSelection: List[Any] = Tiles.entitiesOnTile(tuid.mousedOverHex).toList ::: tuid.mousedOverBiasedHex :: tuid.mousedOverHex :: Nil


		for (ctxt <- SD.activeContext; selector <- ctxt.nextSelector()) {
			val key = Some((toConsiderForSelection, ctxt.entity, ctxt.selectable, selector))
			if (key == lastSelectionContextIfClickedKey) {
				skippedRecalculationCounter.inc()
				return lastSelectionContextIfClicked
			} else {
				for ((_, (satisfying, satisfiedAmount)) <- toConsiderForSelection.findFirstWith(thing => selector.satisfiedBy(view, thing))) {
					lastSelectionContextIfClickedKey = key
					lastSelectionContextIfClicked = Some(ctxt.copy(selectionResults = ctxt.selectionResults.addResult(selector, satisfying, satisfiedAmount)))
					return lastSelectionContextIfClicked
				}
			}
		}

		SD.activeContext
	}

	def nextSelector(view : WorldView, display : World) : Option[Selector[_]] = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]
		SD.activeContext.flatMap(ctxt => {
			tuid.selectedCharacter.flatMap(selC => {
				ctxt.nextSelector()
			})
		})
	}

	def resetSelectionContext(display : World) : Unit = {
		val SD = display[SelectionData]
		SD.activeContext = SD.activeContext.map(ctxt => ctxt.copy(selectionResults = SelectionResult()))
		SD.selectionWidgets.values.foreach(_.destroy())
		SD.selectionWidgets = Map()
	}

	def changeSelectionTarget(game : World, display : World, selectable : Selectable, selectableInst : SelectableInstance, onFinish : SelectionContext => Unit, onCancel : () => Unit): Unit = {
		val SD = display[SelectionData]

		for (selC <- display[TacticalUIData].selectedCharacter) {
			resetSelectionContext(display)
			updateSelectionContext(game.view, display, Some(SelectionContext(selC, selectable, selectableInst, SelectionResult(), onFinish, onCancel)))
		}
	}

	def updateSelectionContext(view : WorldView, display : World, newContext : Option[SelectionContext]) : Unit = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]

		if (SD.activeContext != newContext) {
			SD.activeContext.foreach(ctx => ctx.onCancel())
			SD.activeContext = newContext
			for (selC <- tuid.selectedCharacter) {
				// if we have a selected character, a selection context, and the selection context is fully satisfied, then we're finished
				for (ctxt <- SD.activeContext if ctxt.nextSelector().isEmpty) {
					// when we finish, we want to reset
					ctxt.onFinish(ctxt)
					SD.activeContext = None
				}
			}
		}
	}
}


class SelectionData extends TControlData with TMutableAuxData with TWorldAuxData {
	var activeContext : Option[SelectionContext] = None
	var consideredContext : Option[SelectionContext] = None

	var selectionWidgets : Map[Selector[_], Widget] = Map()
}

case class SelectionContext(val entity : Entity, val selectable : Selectable, val selectableInst : SelectableInstance, val selectionResults : SelectionResult, val onFinish : SelectionContext => Unit, val onCancel : () => Unit) {
	def nextSelector() = selectableInst.nextSelector(selectionResults)
}