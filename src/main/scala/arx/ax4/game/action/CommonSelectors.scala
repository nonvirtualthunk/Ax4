package arx.ax4.game.action

import arx.ai.search.Path
import arx.ax4.game.entities.Companions.Physical
import arx.ax4.game.entities.{HexTargetPattern, Physical, TargetPattern, Tile}
import arx.ax4.game.logic.{AllegianceLogic, AxPathfinder}
import arx.core.vec.coordinates.{AxialVec, AxialVec3, BiasedAxialVec3}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

case class EntitySelector(predicates : Seq[EntityPredicate], description: String, selectable : Selectable) extends Selector[Entity](selectable) {
	override def satisfiedBy(view: WorldView, a: Any): Option[(Entity, Int)] = a match {
		case e: Entity if predicates.forall(p => p.matches(view, e)) => Some(e -> 1)
		case _ => None
	}
}

case class OptionSelector[T](options : Seq[T], selectable : Selectable) extends Selector[T](selectable) {
	override def satisfiedBy(view: WorldView, a: Any): Option[(T, Int)] = {
		if (options.contains(a)) { Some(a.asInstanceOf[T] -> 1) }
		else { None }
	}

	override def description: String = "Select option"
}

trait EntityPredicate {
	def matches(view : WorldView, entity : Entity) : Boolean
}

/**
 * Special predicate indicating that it should select the entity itself
 */
object SelfEntityPredicate extends EntityPredicate {
	override def matches(view: WorldView, entity: Entity): Boolean = true
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
}


case class HexPatternSelector(sourcePoint : AxialVec3, pattern: HexTargetPattern, hexPredicate: (WorldView, BiasedAxialVec3) => Boolean, selectable : Selectable) extends Selector[Vector[AxialVec3]](selectable) {
	override def satisfiedBy(view: WorldView, a: Any): Option[(Vector[AxialVec3], Int)] = a match {
		case bv : BiasedAxialVec3 if hexPredicate(view, bv) => Some(pattern.targetedHexes(sourcePoint, bv.vec).toVector -> 1)
		case _ => None
	}

	override def description: String = "hex with direction"
}