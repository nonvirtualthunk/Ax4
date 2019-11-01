package arx.ax4.control.components
import arx.Prelude
import arx.application.Noto
import arx.ax4.game.action.{AttackAction, GameAction, MoveAction, SelectionResultBuilder}
import arx.ax4.game.entities.Companions.{AllegianceData, Physical}
import arx.ax4.game.entities.{AllegianceData, CharacterInfo, Tile, Tiles, TurnData}
import arx.ax4.game.logic.{AxPathfinder, Movement}
import arx.ax4.graphics.components.TacticalUIData
import arx.core.datastructures.Watcher
import arx.core.units.UnitOfTime
import arx.core.vec.coordinates.AxialVec3
import arx.engine.entity.Entity
import arx.engine.world.{GameEventClock, HypotheticalWorldView, World, WorldView}

class TacticalUIActionControl extends AxControlComponent {

	var watcher : Watcher[(AxialVec3, Int, GameEventClock)] = _

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

		var newPossibleActions = List[GameAction]()
		var newPossibleSelection : Option[Entity] = None

		val activeFaction = gameView.worldData[TurnData].activeFaction
		for (selC <- selectedCharacter) {
			Tiles.characterOnTile(mousedOverHex) match {
				case Some(mousedChar) if mousedChar[AllegianceData].faction == activeFaction =>
					newPossibleSelection = Some(mousedChar)
				case _ =>
					selC[CharacterInfo].activeIntent.instantiate(gameView, selC) match {
						case Right(error) => Noto.warn(s"Intent couldn't be instantiated: $error")
						case Left(intent) =>
							// TODO: Multi step selections
							val selResults = new SelectionResultBuilder

							intent.selections.foreach(selector => {
								selector.satisfiedBy(view, mousedOverHex) match {
									case Some((value, amount)) =>
										selResults.addResult(selector, value, amount)
									case None => // do nothing
								}
							})

							if (intent.selections.forall(selector => selResults.fullySatisfied(selector))) {
								newPossibleActions = intent.createAction(selResults.build()).toList
							}
					}
			}

//			val entitiesInHex = tile.entities
//			if (entitiesInHex.isEmpty) {
//				val pathFound = pathfinder.findPath(selC, selC(Physical).position, AxialVec3(mousedOverHex,0))
//				for (path <- pathFound) {
//					val subPath = Movement.subPath(selC, path, selC[CharacterInfo].curPossibleMovePoints)
//					if (subPath.steps.size < 2) {
//						newPossibleActions = Nil
//					} else {
//						newPossibleActions = List(MoveAction(selC, subPath))
//					}
//				}
//			} else {
//				entitiesInHex.find(e => e.hasData[CharacterInfo]) match {
//					case Some(charInHex) if charInHex(AllegianceData).faction == selC(AllegianceData).faction =>
//						newPossibleSelection = Some(charInHex)
//					case Some(charInHex) if charInHex(AllegianceData).faction != selC(AllegianceData).faction =>
//					//								possibleAction = Some(AttackAction())
//					case None =>
//						Noto.warn("Entities in hex, but no characters in hex, not sure what to do with that yet")
//				}
//			}
		}

		tuid.consideringActions = newPossibleActions
		tuid.consideringSelection = newPossibleSelection
	}
}
