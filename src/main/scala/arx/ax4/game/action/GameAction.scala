package arx.ax4.game.action

import arx.ai.search.Path
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.cardeffects.{CardEffect, CardEffectInstance}
import arx.ax4.game.entities.{AllegianceData, AttackProspect, AttackReference, CharacterInfo, EntityTarget, FactionData, HexTargetPattern, Physical, TargetPattern, Tile, Tiles}
import arx.ax4.game.event.AttackEventInfo
import arx.ax4.game.logic.{AllegianceLogic, AxPathfinder, CharacterLogic, CombatLogic, MovementLogic}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, HexDirection}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.graphics.helpers.{RichText, RichTextRenderSettings}

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

	def displayName(implicit view : WorldView) : String
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
	def hasRemainingSelections(resultsSoFar : SelectionResult) : Boolean = nextSelection(resultsSoFar).isDefined

	def nextSelection(resultsSoFar : SelectionResult) : Option[Selector[_]]

	def createAction(selectionResult: SelectionResult): Seq[GameAction]
}

case object DoNothingIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(new GameActionIntentInstance {
		override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = None
		override def createAction(selectionResult: SelectionResult): Seq[GameAction] = Nil
	})

	override def displayName(implicit view : WorldView): String = "Do Nothing"
}

case object SwitchSelectedCharacterIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(new GameActionIntentInstance {
		val selector = new EntitySelector(Seq(EntityPredicate.Friend(entity)), "Entity with same faction", null)

		override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = if (!resultsSoFar.fullySatisfied(selector)) {
			Some(selector)
		} else {
			None
		}

		override def createAction(selectionResult: SelectionResult): Seq[GameAction] = SwitchSelectedCharacterAction(entity, selectionResult.single(selector)) :: Nil
	})

	override def displayName(implicit view : WorldView): String = "Switch Selected Character"
}

case class AttackIntent(attackRef: AttackReference) extends GameActionIntent {
	override def instantiate(implicit view: WorldView, attacker: Entity): Either[GameActionIntentInstance, String] = {

		attackRef.resolve() match {
			case Some(attack) =>
				val (attackDetails, _) = CombatLogic.resolveUnconditionalAttackData(view, attacker, attackRef.weapon, attack)

				attackDetails.targetPattern match {
					case hexPattern: HexTargetPattern =>
						Left(new GameActionIntentInstance {
							val hexSelector = BiasedHexSelector(attackDetails.targetPattern, (view, v) => true, null)

							override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = Some(hexSelector).filter(h => !resultsSoFar.fullySatisfied(h))

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
									val curMP = CharacterLogic.curMovePoints(attacker)
									if (MovementLogic.movePointsRequiredForPath(attacker, ep) > curMP && MovementLogic.movePointsRequiredForPath(attacker, ap) <= curMP) {
										Some(ap)
									} else {
										Some(ep)
									}
								}

								effectivePath match {
									case Some(path) =>
										val curMP = CharacterLogic.curMovePoints(attacker)
										val pathCost = MovementLogic.movePointsRequiredForPath(attacker, path)
										val subPath = MovementLogic.subPath(attacker, path, curMP)

										var actions = List[GameAction]()
										if (subPath.steps.size >= 2) {
											actions ::= MoveAction(attacker, subPath)
										}
										if (pathCost <= curMP) { // i.e. we can expect to reach our destination with our current mp, so we can attack
											val allHexes = hexPattern.targetedHexes(path.steps.last.node, targetHex.vec)
											actions ::= AttackAction(attacker, attackRef, path.steps.last.node, Right(allHexes), None, None)
										}
										actions.reverse
									case None => Nil
								}
							}
						})
					case entityTarget: EntityTarget =>
						val entitySelector = new EntitySelector(Seq(EntityPredicate.Enemy(attacker), EntityPredicate.InRange(attacker, attack.minRange, attack.maxRange)), "Enemy creature", null).withAmount(entityTarget.count)
						Left(new GameActionIntentInstance {
							override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = Some(entitySelector).filter(h => !resultsSoFar.fullySatisfied(h))

							override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
								val targets = selectionResult(entitySelector)

								AxPathfinder.findPathToMatching(attacker, attacker(Physical).position, v => targets.forall(t => CombatLogic.canAttackBeMade(view, attacker, v, Left(t), attackDetails)))(view) match {
									case Some(path) =>
										val curMP = CharacterLogic.curMovePoints(attacker)
										val pathCost = MovementLogic.movePointsRequiredForPath(attacker, path)
										val subPath = MovementLogic.subPath(attacker, path, curMP)

										var actions = List[GameAction]()
										if (subPath.steps.size >= 2) {
											actions ::= MoveAction(attacker, subPath)
										}
										if (pathCost <= curMP) { // i.e. we can expect to reach our destination with our current mp, so we can attack
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

	override def displayName(implicit view : WorldView): String = {
		attackRef.resolve() match {
			case Some(ad) => ad.name.capitalize
			case None => "Unknown Attack"
		}
	}
}

case object MoveCharacter extends CardEffect {
	def forceInstantiate(world: WorldView, entity : Entity) = MoveCharacterInstance(entity)

	override def instantiate(world: WorldView, entity: Entity): Either[CardEffectInstance, String] = Left(forceInstantiate(world, entity))

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Move")
}

case class MoveCharacterInstance(entity : Entity) extends CardEffectInstance {
	protected case class CustomPathSelector(entity : Entity, selectable : Selectable) extends PathSelector(entity, TargetPattern.Point, selectable) {
		override def hexPredicate(view: WorldView, v: AxialVec3): Boolean = {
			view.hasData[Tile](Tiles.tileAt(v)) && // it is a tile
				! view.data[Tile](Tiles.tileAt(v)).entities.exists(e => view.dataOpt[Physical](e).exists(p => p.occupiesHex))
		}

		override def pathPredicate(view: WorldView, path: Path[AxialVec3]): Boolean = {
			path.steps.size >= 2 &&
				MovementLogic.movePointsRequiredForPath(entity, path)(view) <= CharacterLogic.curMovePoints(entity)(view)
		}
	}

	val pathSelector = CustomPathSelector(entity, MoveCharacter)

	override def applyEffect(world: World, selectionResult: SelectionResult): Unit = {
		val path = selectionResult.single(pathSelector)
		MovementLogic.moveCharacterOnPath(entity, path)(world)
	}

	override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
		if (results.fullySatisfied(pathSelector)) {
			None
		} else {
			Some(pathSelector)
		}
	}
}

case object MoveIntent extends GameActionIntent {
	override def instantiate(implicit view: WorldView, entity: Entity): Either[GameActionIntentInstance, String] = Left(
		new GameActionIntentInstance {
//			val hexSelector = HexSelector(TargetPattern.Point, (view, h) => {
//				view.hasData[Tile](Tiles.tileAt(h)) && // it is a tile
//				! view.data[Tile](Tiles.tileAt(h)).entities.exists(e => view.dataOpt[Physical](e).exists(p => p.occupiesHex)) // the tile is not occupied
//					AxPathfinder.findPath(entity, entity(Physical)(view).position, h)(view).exists(p => p.steps.size >= 2) // there is a path to the tile
//			}, null)
			val hexSelector = new HexSelector(TargetPattern.Point, null) {
				override def hexPredicate(view: WorldView, hex: AxialVec3): Boolean = ???
			}

			override def nextSelection(resultsSoFar: SelectionResult): Option[Selector[_]] = Some(hexSelector).filter(h => !resultsSoFar.fullySatisfied(h))

			override def createAction(selectionResult: SelectionResult): Seq[GameAction] = {
				val hex = selectionResult(hexSelector)
				val pathFound = AxPathfinder.findPath(entity, entity(Physical).position, hex)
				pathFound match {
					case Some(path) =>
						val subPath = MovementLogic.subPath(entity, path, CharacterLogic.curMovePoints(entity))
						if (subPath.steps.size < 2) {
							Nil
						} else {
							List(MoveAction(entity, subPath))
						}
					case None =>
						Noto.error("No path found when trying to create move action from intent")
						Nil
				}
			}
		}
	)

	override def displayName(implicit view: WorldView): String = "Move"
}



case class SelectionResult(results : Map[Selector[_], List[Any]] = Map(),
									amountSatisfied : Map[Selector[_], Int] = Map())
{

	def addResult(selector : Selector[_], result: Any, amount: Int): SelectionResult = {
		SelectionResult(
			results + (selector -> (result :: results.getOrElse(selector, Nil))),
			amountSatisfied + (selector -> (amountSatisfied.getOrElse(selector, 0) + amount))
		)
	}

	def setResults(selector: Selector[_], newResults: List[Any], amount: Int) = {
		SelectionResult(
			results + (selector -> newResults),
			amountSatisfied + (selector -> amount)
		)
	}

	def fullySatisfied(selector : Selector[_]) : Boolean = amountSatisfied.getOrElse(selector, 0) >= selector.amount
	def fullySatisfied[A <: Selector[_],B <: Selector[_]](selector : Either[A,B]) : Boolean = {
		selector match {
			case Left(value) => fullySatisfied(value)
			case Right(value) => fullySatisfied(value)
		}
	}

	def selectedFor(selector : Selector[_]) : List[Any] = results.getOrElse(selector, Nil)

	def isEmpty = results.isEmpty

	def apply[T](sel: Selector[T]): List[T] = {
		results.getOrElse(sel, Nil).asInstanceOf[List[T]]
	}
	def apply[A,B](sel: Either[Selector[A], Selector[B]]): Either[List[A], List[B]] = {
		sel match {
			case Left(value) => Left(this.apply(value))
			case Right(value) => Right(this.apply(value))
		}
	}

	def single[T](sel: Selector[T]): T = apply(sel).head
}

trait Selectable {
	def instantiate(world : WorldView, entity : Entity) : Either[SelectableInstance, String]
}

trait SelectableInstance {
	def nextSelector(results : SelectionResult) : Option[Selector[_]]
}



trait CompoundSelectable extends Selectable {
	def subSelectables(world : WorldView) : Traversable[Selectable]

	override def instantiate(world: WorldView, entity: Entity): Either[SelectableInstance, String] = {
		val subSel = subSelectables(world)
		val subSelectableInstRaw = subSel.map(s => s -> s.instantiate(world, entity))

		var subSelectableInst = Vector[(Selectable, SelectableInstance)]()
		subSelectableInstRaw.foreach {
			case (k, Left(s)) => subSelectableInst :+= (k -> s)
			case (_, Right(reason)) => return Right(reason)
		}

		Left(new CompoundSelectableInstance(subSelectableInst))
	}
}

class CompoundSelectableInstance(val subSelectableInstances : Vector[(Selectable, SelectableInstance)]) extends SelectableInstance {
	override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
		for ((_, selInst) <- subSelectableInstances) {
			selInst.nextSelector(results) match {
				case s @ Some(_) => return s
				case None => // continue
			}
		}
		None
	}
}

abstract class Selector[MatchedType](val origin : Selectable) {
	var amount = 1

	def satisfiedBy(view: WorldView, a: Any): Option[(MatchedType, Int)]

	def description: String

	def withAmount(n: Int) = {
		amount = n
		this
	}
}

case class EntitySelector(predicates : Seq[EntityPredicate], description: String, selectable : Selectable) extends Selector[Entity](selectable) {


	override def satisfiedBy(view: WorldView, a: Any): Option[(Entity, Int)] = a match {
		case e: Entity if predicates.forall(p => p.matches(view, e)) => Some(e -> 1)
		case _ => None
	}
}

trait EntityPredicate {
	def matches(view : WorldView, entity : Entity) : Boolean
}

object EntityPredicate {
	case class Enemy(source : Entity) extends EntityPredicate {
		override def matches(view: WorldView, entity: Entity): Boolean = AllegianceLogic.areEnemies(source, entity)(view)
	}
	case class InRange(source : Entity, minRange : Int, maxRange : Int) extends EntityPredicate {
		override def matches(view: WorldView, entity: Entity): Boolean = {
			implicit val v = view
			(entity.dataOpt[Physical], source.dataOpt[Physical]) match {
				case (Some(from), Some(to)) => {
					val dist = from.position.distance(to.position)
					dist >= minRange && dist <= maxRange
				}
				case _ => false
			}
		}
	}

	case class Friend(source : Entity) extends EntityPredicate {
		override def matches(view: WorldView, entity: Entity): Boolean = AllegianceLogic.areInSameFaction(entity, source)(view)
	}

}


object EntitySelector {
	def Enemy(attacker : Entity, selectable : Selectable) = new EntitySelector(Vector(EntityPredicate.Enemy(attacker)),"Enemy Creature", selectable)
}

abstract class HexSelector(pattern: TargetPattern, selectable : Selectable) extends Selector[AxialVec3](selectable) {
	def hexPredicate(view : WorldView, hex : AxialVec3) : Boolean

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

object HexSelector {
	def apply(pattern : TargetPattern, selectable : Selectable, predicate : AxialVec3 => Boolean) : HexSelector = new HexSelector(pattern, selectable) {
		override def hexPredicate(view: WorldView, hex: AxialVec3): Boolean = predicate(hex)
	}
}

/**
 * Selecting a destination and a path to travel to it
 */
abstract class PathSelector(entity : Entity, pattern : TargetPattern, selectable : Selectable) extends Selector[Path[AxialVec3]](selectable) {
	def hexPredicate(view : WorldView, v : AxialVec3) : Boolean
	def pathPredicate(view : WorldView, path : Path[AxialVec3]) : Boolean

	override def satisfiedBy(view: WorldView, a: Any): Option[(Path[AxialVec3], Int)] = {
		val target : Option[AxialVec3] = a match {
			case v3: AxialVec3 => Some(v3)
			case v2: AxialVec => Some(AxialVec3(v2, 0))
			case e: Entity => view.dataOpt[Tile](e) match {
				case Some(tile) => Some(tile.position)
				case None => None
			}
			case _ => None
		}

		target.filter(v3 => hexPredicate(view, v3)) match {
			case Some(validTarget) => AxPathfinder.findPath(entity, entity(Physical)(view).position, validTarget)(view) match {
				case Some(validPath) if pathPredicate(view, validPath) => Some(validPath -> 1)
				case _ => None
			}
			case _ => None
		}
	}

	override def description: String = "path to hex"

//	override def hashCode(): Int = (entity, pattern).hashCode()
//
//	override def equals(obj: Any): Boolean = obj match {
//		case PathSelector(otherEnt, otherPattern, _, _) => entity == otherEnt && pattern == otherPattern
//		case _ => false
//	}
}


case class BiasedHexSelector(pattern: TargetPattern, hexPredicate: (WorldView, BiasedAxialVec3) => Boolean, selectable : Selectable) extends Selector[BiasedAxialVec3](selectable) {
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