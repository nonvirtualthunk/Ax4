package arx.ax4.control.components
import arx.ax4.game.action.{MoveCharacter, Selectable, SelectionResult, Selector}
import arx.ax4.game.entities.{AllegianceData, FactionData, Tiles, TurnData}
import arx.ax4.graphics.data.TacticalUIData
import arx.core.units.UnitOfTime
import arx.engine.control.components.windowing.Widget
import arx.engine.control.data.{TControlData, WindowingControlData}
import arx.engine.entity.Entity
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.Prelude._
import arx.ax4.game.logic.AllegianceLogic
import arx.ax4.graphics.components.subcomponents.TacticalSelectableRenderer
import arx.core.introspection.ReflectionAssistant
import arx.engine.data.{TMutableAuxData, TWorldAuxData}

class SelectionControl(mainControl : TacticalUIControl) extends AxControlComponent {
	lazy val selectableRenderers = ReflectionAssistant.instancesOfSubtypesOf[TacticalSelectableRenderer]

	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		val SD = display[SelectionData]

		for (selC <- display[TacticalUIData].selectedCharacter) {
			if (SD.activeContext.isEmpty || SD.activeContext.get.entity != selC) {
				SD.activeContext = Some(SelectionContext(selC, MoveCharacter, SelectionResult(), sc => MoveCharacter.applyEffect(game, sc.entity, sc.selectionResults)))
			}
		}

		SD.consideredContext = selectionContextIfClicked(game.view, display)

		val desktop = display[WindowingControlData].desktop
		for (selC <- display[TacticalUIData].selectedCharacter; ctxt <- SD.consideredContext) {
			selectableRenderers.foreach(r => r.updateUI(gameView, display, selC, ctxt.selectable, ctxt.selectionResults, desktop))
			selectableRenderers.foreach(r => r.update(gameView, display, selC, ctxt.selectable, ctxt.selectionResults))
		}
	}

	override protected def onInitialize(view: HypotheticalWorldView, game: World, display: World): Unit = {
		val SD = display[SelectionData]
		implicit val view = game.view

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
		}
	}

	def selectionContextIfClicked(implicit view : WorldView, display : World) : Option[SelectionContext] = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]


		for (ctxt <- SD.activeContext; selector <- ctxt.nextSelector(view)) {
			val toConsiderForSelection: List[Any] = Tiles.entitiesOnTile(tuid.mousedOverHex).toList ::: tuid.mousedOverBiasedHex :: tuid.mousedOverHex :: Nil
			for ((_, (satisfying, satisfiedAmount)) <- toConsiderForSelection.findFirstWith(thing => selector.satisfiedBy(view, thing))) {
				return Some(ctxt.copy(selectionResults = ctxt.selectionResults.addResult(selector, satisfying, satisfiedAmount)))
			}
		}
		SD.activeContext
	}

	def nextSelector(view : WorldView, display : World) : Option[Selector[_]] = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]
		SD.activeContext.flatMap(ctxt => {
			tuid.selectedCharacter.flatMap(selC => {
				ctxt.selectable.nextSelector(view,selC,ctxt.selectionResults)
			})
		})
	}

	def resetSelectionContext(display : World) : Unit = {
		val SD = display[SelectionData]
		SD.activeContext = SD.activeContext.map(ctxt => ctxt.copy(selectionResults = SelectionResult()))
		SD.selectionWidgets.values.foreach(_.destroy())
		SD.selectionWidgets = Map()
	}

	def changeSelectionTarget(display : World, selectable : Selectable, onFinish : SelectionContext => Unit): Unit = {
		val SD = display[SelectionData]

		for (selC <- display[TacticalUIData].selectedCharacter) {
			SD.activeContext = Some(SelectionContext(selC, selectable, SelectionResult(), onFinish))
			resetSelectionContext(display)
		}
	}

	def updateSelectionContext(view : WorldView, display : World, newContext : Option[SelectionContext]) : Unit = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]

		if (SD.activeContext != newContext) {
			SD.activeContext = newContext
			for (selC <- tuid.selectedCharacter) {
				// if we have a selected character, a selection context, and the selection context is fully satisfied, then we're finished
				for (ctxt <- SD.activeContext if ctxt.nextSelector(view).isEmpty) {
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

case class SelectionContext(val entity : Entity, val selectable : Selectable, val selectionResults : SelectionResult, val onFinish : SelectionContext => Unit) {
	def nextSelector(view : WorldView) = selectable.nextSelector(view, entity, selectionResults)
}