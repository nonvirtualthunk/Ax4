package arx.ax4.game.action

import arx.ai.search.Path
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.{AllegianceData, AttackProspect, AttackReference, CharacterInfo, EntityTarget, FactionData, HexTargetPattern, Physical, TargetPattern, Tile, Tiles}
import arx.ax4.game.event.AttackEventInfo
import arx.ax4.game.logic.{AllegianceLogic, AxPathfinder, CombatLogic, MovementLogic}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, HexDirection}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView

abstract class GameAction {
	def identity: Taxon

	def entity: Entity
}

case class DoNothingAction(character: Entity) extends GameAction {
	val identity: Taxon = Taxonomy("DoNothing", "Actions")

	override def entity: Entity = character
}

case class MoveAction(character: Entity, path: Path[AxialVec3]) extends GameAction {
	val identity: Taxon = Taxonomy("MoveAction", "Actions")

	override def entity: Entity = character
}

case class AttackAction(attacker: Entity,
								attack: AttackReference,
								from : AxialVec3,
								targets: Either[Seq[Entity], Seq[AxialVec3]],
								preMove: Option[Path[AxialVec3]],
								postMove: Option[Path[AxialVec3]]) extends GameAction {
	val identity: Taxon = Taxonomy("AttackAction", "Actions")

	override def entity: Entity = attacker
}


case class SwitchSelectedCharacterAction(from : Entity, to : Entity) extends GameAction {
	val identity: Taxon = Taxonomy("SwitchSelectedCharacterAction", "Actions")

	override def entity: Entity = from
}

abstract class GameActionIntent {
	def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String]
}


/*
How do we deal with a selection that is dependent on previous selections. In specific, gathering from a hex, choosing
which resource to gather depends on which hex you are targeting in the first place. The alternative would be to put
some special purpose logic in to handle that. I.e. we just have a ResourceSelector and have the ui be made aware
of the limiting there.

Or we do it right, and we make selections into something like nextSelection and give the intent instance a built in
SelectionResultBuilder
 */
abstract class GameActionIntentInstance {
	def hasRemainingSelections(resultsSoFar : SelectionResultBuilder) : Boolean = nextSelection(resultsSoFar).isDefined

	def nextSelection(resultsSoFar : SelectionResultBuilder) : Option[Selector[_]]

	def createAction(selectionResult: SelectionResult): Seq[GameAction]
}

case object DoNothingIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(new GameActionIntentInstance {
		override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = None
		override def createAction(selectionResult: SelectionResult): Seq[GameAction] = Nil
	})
}

case object SwitchSelectedCharacterIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(new GameActionIntentInstance {
		val selector = EntitySelector((view, other) => AllegianceLogic.areInSameFaction(entity, other)(view), "Entity with same faction")

		override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = if (!resultsSoFar.fullySatisfied(selector)) {
			Some(selector)
		} else {
			None
		}

		override def createAction(selectionResult: SelectionResult): Seq[GameAction] = SwitchSelectedCharacterAction(entity, selectionResult.single(selector)) :: Nil
	})
}

case class AttackIntent(attackRef: AttackReference) extends GameActionIntent {
	override def instantiate(implicit view: WorldView, attacker: Entity): Either[GameActionIntentInstance, String] = {

		def getApAfterMove(p : Path[AxialVec3]) = {
			attacker(CharacterInfo).actionPoints.currentValue - MovementLogic.actionPointsRequiredForPath(attacker, p)
		}

		attackRef.resolve() match {
			case Some(attack) =>
				val (attackDetails, _) = CombatLogic.resolveUnconditionalAttackData(view, attacker, attackRef.weapon, attack)

				attackDetails.targetPattern match {
					case hexPattern: HexTargetPattern =>
						Left(new GameActionIntentInstance {
							val hexSelector = BiasedHexSelector(attackDetails.targetPattern, (view, v) => true)

							override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = Some(hexSelector).filter(h => !resultsSoFar.fullySatisfied(h))

							override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
								val targetHex = selectionResult.single(hexSelector)

								val attackerPos = attacker(Physical).position
								val exactDirectionPath = AxPathfinder.findPathToMatching(attacker, attackerPos, v => {
									targetHex.vec.sideClosestTo(v, AxialVec3.Zero) == targetHex.biasDirection &&
									CombatLogic.canAttackBeMade(view, attacker, v, Right(targetHex.vec), attackDetails)
								})(view)
								lazy val approximateDirectionPath = AxPathfinder.findPathToMatching(attacker, attackerPos, v => {
									CombatLogic.canAttackBeMade(view, attacker, v, Right(targetHex.vec), attackDetails)
								})(view)

								val effectivePath = if(exactDirectionPath.isEmpty) {
									approximateDirectionPath
								} else if (approximateDirectionPath.isEmpty ) {
									exactDirectionPath
								} else {
									val ep = exactDirectionPath.get
									val ap = approximateDirectionPath.get
									if (getApAfterMove(ep) < attackDetails.actionCost && getApAfterMove(ap) >= attackDetails.actionCost) {
										Some(ap)
									} else {
										Some(ep)
									}
								}

								effectivePath match {
									case Some(path) =>
										val subPath = MovementLogic.subPath(attacker, path, attacker[CharacterInfo].curPossibleMovePoints)
										val apAfterMove = getApAfterMove(subPath)

										var actions = List[GameAction]()
										if (path.steps.size >= 2) {
											actions ::= MoveAction(attacker, subPath)
										}
										if (apAfterMove >= attackDetails.actionCost) {
											val allHexes = hexPattern.targetedHexes(path.steps.last.node, targetHex.vec)
											actions ::= AttackAction(attacker, attackRef, path.steps.last.node, Right(allHexes), None, None)
										}
										actions.reverse
									case None => Nil
								}
							}
						})
					case entityTarget: EntityTarget =>
						val entitySelector = EntitySelector((view, ent) => AllegianceLogic.areEnemies(attacker, ent)(view), "Enemy creature")
							.withAmount(entityTarget.count)
						Left(new GameActionIntentInstance {
							override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = Some(entitySelector).filter(h => !resultsSoFar.fullySatisfied(h))

							override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
								val targets = selectionResult(entitySelector)

								AxPathfinder.findPathToMatching(attacker, attacker(Physical).position, v => targets.forall(t => CombatLogic.canAttackBeMade(view, attacker, v, Left(t), attackDetails)))(view) match {
									case Some(path) =>
										val subPath = MovementLogic.subPath(attacker, path, attacker[CharacterInfo].curPossibleMovePoints)
										val apAfterMove = attacker(CharacterInfo).actionPoints.currentValue - MovementLogic.actionPointsRequiredForPath(attacker, subPath)

										var actions = List[GameAction]()
										if (path.steps.size >= 2) {
											actions ::= MoveAction(attacker, subPath)
										}
										if (apAfterMove >= attackDetails.actionCost) {
											actions ::= AttackAction(attacker, attackRef, path.steps.last.node, Left(targets), None, None)
										}
										actions.reverse
									case None => Nil
								}
							}
						})
				}
			case None => Right("attack could not be resolved")
		}
	}
}

case object MoveIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(
		new GameActionIntentInstance {
			val hexSelector = HexSelector(TargetPattern.Point, (view, h) => {
				view.hasData[Tile](Tiles.tileAt(h)) && // it is a tile
				! view.data[Tile](Tiles.tileAt(h)).entities.exists(e => view.dataOpt[Physical](e).exists(p => p.occupiesHex)) // the tile is not occupied
					AxPathfinder.findPath(entity, entity(Physical)(view).position, h)(view).exists(p => p.steps.size >= 2) // there is a path to the tile
			})

			override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = Some(hexSelector).filter(h => !resultsSoFar.fullySatisfied(h))

			override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
				val hex = selectionResult(hexSelector)
				val pathFound = AxPathfinder.findPath(entity, entity(Physical).position, hex)
				pathFound match {
					case Some(path) =>
						val subPath = MovementLogic.subPath(entity, path, entity[CharacterInfo].curPossibleMovePoints)
						if (subPath.steps.size < 2) {
							List(DoNothingAction(entity))
						} else {
							List(MoveAction(entity, subPath))
						}
					case None =>
						Noto.error("No path found when trying to create move action from intent")
						List(DoNothingAction(entity))
				}
			}
		}
	)
}


case object WaypointMoveIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(
		new GameActionIntentInstance {
			val hexSelector1 = HexSelector(TargetPattern.Point, (view, h) => {
				view.hasData[Tile](Tiles.tileAt(h)) && // it is a tile
					! view.data[Tile](Tiles.tileAt(h)).entities.exists(e => view.dataOpt[Physical](e).exists(p => p.occupiesHex)) // the tile is not occupied
				AxPathfinder.findPath(entity, entity(Physical)(view).position, h)(view).exists(p => p.steps.size >= 2) // there is a path to the tile
			})

			val hexSelector2 = HexSelector(TargetPattern.Point, (view, h) => {
				view.hasData[Tile](Tiles.tileAt(h)) && // it is a tile
					! view.data[Tile](Tiles.tileAt(h)).entities.exists(e => view.dataOpt[Physical](e).exists(p => p.occupiesHex)) // the tile is not occupied
				AxPathfinder.findPath(entity, entity(Physical)(view).position, h)(view).exists(p => p.steps.size >= 2) // there is a path to the tile
			})

			override def nextSelection(resultsSoFar: SelectionResultBuilder): Option[Selector[_]] = {
				List(hexSelector1, hexSelector2).find(s => !resultsSoFar.fullySatisfied(s))
			}

			override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
				var lastStartPos = entity(Physical).position

				List(hexSelector1, hexSelector2).flatMap(sel => {
					val startPos = lastStartPos
					val hex = selectionResult(sel)
					lastStartPos = hex.head
					val pathFound = AxPathfinder.findPath(entity, startPos, hex)
					pathFound match {
						case Some(path) =>
							val subPath = MovementLogic.subPath(entity, path, entity[CharacterInfo].curPossibleMovePoints)
							if (subPath.steps.size < 2) {
								List(DoNothingAction(entity))
							} else {
								List(MoveAction(entity, subPath))
							}
						case None =>
							Noto.error("No path found when trying to create move action from intent")
							List(DoNothingAction(entity))
					}
				})
			}
		}
	)
}



case class SelectionResult(private val results: Map[Selector[_], List[Any]] = Map()) {
	def apply[T](sel: Selector[T]): List[T] = {
		results(sel).asInstanceOf[List[T]]
	}

	def single[T](sel: Selector[T]): T = apply(sel).head
}

case class SelectionResultBuilder(results : Map[Selector[_], List[Any]] = Map(),
											 amountSatisfied : Map[Selector[_], Int] = Map())
{

	def addResult(selector : Selector[_], result: Any, amount: Int): SelectionResultBuilder = {
		SelectionResultBuilder(
			results + (selector -> (result :: results.getOrElse(selector, Nil))),
			amountSatisfied + (selector -> (amountSatisfied.getOrElse(selector, 0) + amount))
		)
	}

	def setResults(selector: Selector[_], newResults: List[Any], amount: Int) = {
		SelectionResultBuilder(
			results + (selector -> newResults),
			amountSatisfied + (selector -> amount)
		)
	}

	def fullySatisfied(selector : Selector[_]) : Boolean = amountSatisfied.getOrElse(selector, 0) >= selector.amount

	def selectedFor(selector : Selector[_]) : List[Any] = results.getOrElse(selector, Nil)

	def build() : SelectionResult = new SelectionResult(results)

	def isEmpty = results.isEmpty

}


trait Selector[MatchedType] {
	var amount = 1

	def satisfiedBy(view: WorldView, a: Any): Option[(MatchedType, Int)]

	def description: String

	def withAmount(n: Int) = {
		amount = n
		this
	}
}

case class EntitySelector(predicate: (WorldView, Entity) => Boolean, description: String) extends Selector[Entity] {
	override def satisfiedBy(view: WorldView, a: Any): Option[(Entity, Int)] = a match {
		case e: Entity if predicate(view, e) => Some(e -> 1)
		case _ => None
	}
}

case class HexSelector(pattern: TargetPattern, hexPredicate: (WorldView, AxialVec3) => Boolean) extends Selector[AxialVec3] {
	override def satisfiedBy(view: WorldView, a: Any): Option[(AxialVec3, Int)] = a match {
		case v3: AxialVec3 if hexPredicate(view, v3) => Some(v3 -> 1)
		case v2: AxialVec => satisfiedBy(view, AxialVec3(v2, 0))
		case e: Entity => view.dataOpt[Tile](e) match {
			case Some(tile) if hexPredicate(view, tile.position) => Some(tile.position -> 1)
			case None => None
		}
		case _ => None
	}

	override def description: String = "hex"
}


case class BiasedHexSelector(pattern: TargetPattern, hexPredicate: (WorldView, BiasedAxialVec3) => Boolean) extends Selector[BiasedAxialVec3] {
	override def satisfiedBy(view: WorldView, a: Any): Option[(BiasedAxialVec3, Int)] = a match {
		case bv : BiasedAxialVec3 if hexPredicate(view, bv) => Some(bv -> 1)
		case _ => None
	}

	override def description: String = "hex with direction"
}


case class BiasedAxialVec3(vec : AxialVec3, biasDirection : HexDirection)


/*
Intent(AttackReference, selections : Map("targetHex" -> HexSelection))

Intent(SpellReference, selections : Map(
 */