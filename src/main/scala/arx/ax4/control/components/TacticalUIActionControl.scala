package arx.ax4.control.components
import arx.application.Noto
import arx.ax4.game.action.{GameActionIntent, GameActionIntentInstance, MoveIntent, SelectionResultBuilder}
import arx.ax4.game.entities.{AllegianceData, CharacterInfo, Tiles, TurnData}
import arx.ax4.graphics.data.TacticalUIData
import arx.core.CachedKeyedValue
import arx.core.datastructures.Watcher
import arx.core.units.UnitOfTime
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World}

class TacticalUIActionControl extends AxControlComponent {
	import TacticalUIActionControl._

	var watcher : Watcher[(AxialVec3, Int, GameEventClock)] = _

	var selectionContext = new CachedKeyedValue[ActionSelectionCacheKey, ActionSelectionContext]

	var lastSelectionContextKey : Option[ActionSelectionCacheKey] = None


	override protected def onUpdate(gameView: HypotheticalWorldView, game: World, display: World, dt: UnitOfTime): Unit = {
		if (watcher.hasChanged || gameView.currentTime != game.currentTime) {
			updateConsideredActions(gameView, game, display)
		}
	}

	override protected def onInitialize(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root

		val tuid = display[TacticalUIData]
		watcher = Watcher((tuid.mousedOverHex, tuid.mousedOverHexBiasDir, gameView.currentTime))
	}

	def updateConsideredActions(gameView : HypotheticalWorldView, game: World, display: World): Unit = {
		implicit val view = gameView.root // take the non-hypothetical, but still slowly advancing world view
		val tuid = display[TacticalUIData]
		import tuid._

		var newConsideredActionSelectionContext : Option[ActionSelectionContext] = None
		var newPossibleSelection : Option[Entity] = None

		val activeFaction = gameView.worldData[TurnData].activeFaction
		selectedCharacter match {
			case Some(selC) =>
				val activeIntent = selC[CharacterInfo].activeIntent
				val selKey = ActionSelectionCacheKey(selC, activeIntent, gameView.currentTime)

				// set up the fixed selection context. That is, what has actually been selected as opposed to what is being considered as a next possible
				// selection
				if (!lastSelectionContextKey.contains(selKey) || tuid.actionSelectionContext.isEmpty) {
					tuid.actionSelectionContext = activeIntent.instantiate(gameView, selC) match {
						case Left(intentInstance) =>
							Some(ActionSelectionContext(intentInstance, SelectionResultBuilder()))
						case Right(error) =>
							Noto.error(s"Could not instantiate selection context: $error")
							None
					}
					lastSelectionContextKey = Some(selKey)
				}
				newConsideredActionSelectionContext = actionSelectionContext

				actionSelectionContext match {
					case Some(ActionSelectionContext(intent, selectionResults)) =>
						var selectionIdentified = false
						intent.nextSelection(selectionResults) match {
							case Some(sel) =>
								val toConsiderForSelection : List[Any] = Tiles.entitiesOnTile(mousedOverHex).toList ::: mousedOverBiasedHex :: mousedOverHex :: Nil
								for (thing <- toConsiderForSelection; if ! selectionIdentified) {
									val consideredSelectionResults = sel.satisfiedBy(view, thing) match {
										case Some((satisfyingThing, amount)) => selectionResults.addResult(sel, satisfyingThing, amount)
										case _ => selectionResults
									}
									if (consideredSelectionResults != selectionResults) {
										selectionIdentified = true
										newConsideredActionSelectionContext = Some(ActionSelectionContext(intent, consideredSelectionResults))
									}
								}
							case None => // do nothing, fully selected already
						}




						// if we haven't identified anything, see if this can be a character switch
						if (! selectionIdentified) {
							for (mousedEnt <- Tiles.entitiesOnTile(mousedOverHex)) {
								if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction)) {
									newPossibleSelection = Some(mousedEnt)
									selectionIdentified = true
								}
							}
						}
						// if we still haven't identified anything, see if this can be a simple move. Only consider this if we're not in the middle
						// of selecting something more complicated
						if (!selectionIdentified && (actionSelectionContext.isEmpty || actionSelectionContext.exists(asc => asc.selectionResults.isEmpty))) {
							MoveIntent.instantiate(view, selC) match {
								case Left(moveIntent) =>
									var selectionResultBuilder = SelectionResultBuilder()
									for (thing <- List(mousedOverBiasedHex, mousedOverHex); sel <- moveIntent.nextSelection(selectionResultBuilder)) {
										sel.satisfiedBy(view, thing) match {
											case Some((value, amount)) => selectionResultBuilder = selectionResultBuilder.addResult(sel, value, amount)
											case None => // do nothing
										}
									}
									if (moveIntent.nextSelection(selectionResultBuilder).isEmpty) {
										newConsideredActionSelectionContext = Some(ActionSelectionContext(moveIntent, selectionResultBuilder))
									}
								case Right(_) => // do nothing
							}
						}
					case None => // if we can't set up a context for whatever reason, fall back on allowing switching active character
						for (mousedEnt <- Tiles.entitiesOnTile(mousedOverHex)) {
							if (mousedEnt.dataOpt[AllegianceData].exists(ad => ad.faction == activeFaction)) {
								newPossibleSelection = Some(mousedEnt)
							}
						}
				}
			case None =>
				tuid.actionSelectionContext = None
		}

		tuid.consideringActionSelectionContext = newConsideredActionSelectionContext
		tuid.consideringSelection = newPossibleSelection
	}
}

object TacticalUIActionControl {
	case class ActionSelectionCacheKey(entity : Entity, intent : GameActionIntent, time : GameEventClock)

}

case class ActionSelectionContext(intent : GameActionIntentInstance, selectionResults: SelectionResultBuilder)