package arx.ax4.game.action

import arx.ai.search.Path
import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, Physical}
import arx.ax4.game.entities.cardeffects.{GameEffect, GameEffectInstance}
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


case object MoveCharacter extends GameEffect {
	def forceInstantiate(world: WorldView, entity : Entity) = MoveCharacterInstance(entity)

	override def instantiate(world: WorldView, entity: Entity): Either[GameEffectInstance, String] = Left(forceInstantiate(world, entity))

	override def toRichText(settings: RichTextRenderSettings): RichText = RichText("Move")
}

case class MoveCharacterInstance(entity : Entity) extends GameEffectInstance {
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


/*
Intent(AttackReference, selections : Map("targetHex" -> HexSelection))

Intent(SpellReference, selections : Map(
 */