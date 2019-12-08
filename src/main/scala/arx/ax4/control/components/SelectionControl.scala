package arx.ax4.control.components
import arx.ax4.game.action.{Selectable, SelectionResult, Selector}
import arx.ax4.game.entities.Tiles
import arx.ax4.graphics.data.TacticalUIData
import arx.core.units.UnitOfTime
import arx.engine.control.components.windowing.Widget
import arx.engine.control.data.TControlData
import arx.engine.entity.Entity
import arx.engine.world.{HypotheticalWorldView, World, WorldView}
import arx.Prelude._

class SelectionControl extends AxControlComponent {
	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(view: HypotheticalWorldView, game: World, display: World): Unit = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]
		implicit val view = game.view

		onControlEvent {
			case HexMouseReleaseEvent(button, hex, pos, modifiers) =>
				SD.activeContext = selectionContextIfClicked(view, display)
		}
	}

	def selectionContextIfClicked(implicit view : WorldView, display : World) : Option[SelectionContext] = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]


		for (ctxt <- SD.activeContext; selC <- tuid.selectedCharacter; selector <- ctxt.nextSelector(view, selC)) {
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

		SD.activeContext = Some(SelectionContext(selectable, SelectionResult(), onFinish))
		resetSelectionContext(display)
	}

	def updateSelectionContext(view : WorldView, display : World, newContext : Option[SelectionContext]) : Unit = {
		val SD = display[SelectionData]
		val tuid = display[TacticalUIData]

		if (SD.activeContext != newContext) {
			SD.activeContext = newContext
			for (selC <- tuid.selectedCharacter) {
				// if we have a selected character, a selection context, and the selection context is fully satisfied, then we're finished
				for (ctxt <- SD.activeContext if ctxt.nextSelector(view, selC).isEmpty) {
					// when we finish, we want to reset
					ctxt.onFinish(ctxt)
					resetSelectionContext(display)
				}
			}
		}
	}
}


private class SelectionData extends TControlData {
	var activeContext : Option[SelectionContext] = None

	var selectionWidgets : Map[Selector[_], Widget] = Map()
}

case class SelectionContext(val selectable : Selectable, val selectionResults : SelectionResult, val onFinish : SelectionContext => Unit) {
	def nextSelector(view : WorldView, entity : Entity) = selectable.nextSelector(view, entity, selectionResults)
}