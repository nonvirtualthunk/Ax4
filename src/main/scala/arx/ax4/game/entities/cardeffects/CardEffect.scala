package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{HexSelector, ResourceGatherSelector, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.Physical
import arx.ax4.game.entities.{CardSelector, DeckData, TargetPattern, Tiles}
import arx.ax4.game.logic.{CardLogic, CharacterLogic, GatherLogic}
import arx.core.introspection.Field
import arx.core.introspection.FieldOperations.{Add, Sub}
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.data.TAuxData
import arx.engine.entity.Entity
import arx.engine.world.{FieldOperationModifier, World, WorldView}

import scala.reflect.ClassTag

trait CardEffect {
	def nextSelector(world : WorldView, entity : Entity, results : SelectionResult) : Option[Selector[_]]
	def applyEffect(world : World, entity : Entity, selectionResult: SelectionResult)
	def canApplyEffect(world : WorldView, entity: Entity) : Boolean
}

case class Gather(range : Int) extends CardEffect {
	def hexSelector(entity : Entity) = HexSelector(TargetPattern.Point, (view,v) => {
		v.distance(entity(Physical)(view).position) <= range && GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v))(view).nonEmpty
	})
	def prospectSelector(view : WorldView, entity : Entity, target : AxialVec3) = ResourceGatherSelector(GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(target))(view))


	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = {
		val hexSel = hexSelector(entity)
		if (!results.fullySatisfied(hexSel)) {
			Some(hexSel)
		} else {
			val hex = results.single(hexSel)
			val rsrcSel = prospectSelector(world, entity, hex)
			if (results.fullySatisfied(rsrcSel)) {
				None
			} else {
				Some(rsrcSel)
			}
		}
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		val hex = selectionResult.single(hexSelector(entity))
		val prospect = selectionResult.single(prospectSelector(world.view, entity, hex))

		prospect.toGatherProspect(world.view) match {
			case Some(p) => GatherLogic.gather(p)(world)
			case None => Noto.error(s"Gather failed after being selected: $prospect")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		implicit val view = world
		val center = entity(Physical).position
		(0 to range).exists { r => HexRingIterator(center, r).exists(v => {GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v)).nonEmpty}) }
	}
}

case class GainMove(mp : Int) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = None

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		CharacterLogic.gainMovePoints(entity, mp)(world)
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = true
}



case class DiscardCards(numCards : Int, random : Boolean) extends CardEffect {
	val cardSelector = CardSelector(_ => true, "Card to discard").withAmount(numCards)

	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) {
		None
	} else {
		Some(cardSelector)
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		world.dataOpt[DeckData](entity) match {
			case Some(_) =>
				val cards = selectionResult(cardSelector)
				CardLogic.discardCards(entity, cards, explicit = true)(world)
			case None =>
				Noto.error("No deck to discard from")
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = {
		world.dataOpt[DeckData](entity) match {
			case Some(deck) => deck.hand.size >= numCards
			case None => false
		}
	}
}

//case class IncreaseFlag(flag : Taxon, n : Int) extends CardEffect
//case class DecreaseFlag(flag : Taxon, n : Int) extends CardEffect

case class AddValue[C <: TAuxData, T : Numeric](field : Field[C,T], amount : T, to : Selector[Entity])(implicit tag : ClassTag[C]) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = {
		if (results.fullySatisfied(to)) {
			None
		} else {
			Some(to)
		}
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		for (target <- selectionResult(to)) {
			world.modify[C](target, FieldOperationModifier[C,T](field, Add(amount)))(tag)
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = true
}

case class SubValue[C <: TAuxData, T : Numeric](field : Field[C,T], amount : T, to : Selector[Entity])(implicit tag : ClassTag[C]) extends CardEffect {
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = {
		if (results.fullySatisfied(to)) {
			None
		} else {
			Some(to)
		}
	}

	override def applyEffect(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		for (target <- selectionResult(to)) {
			world.modify[C](target, FieldOperationModifier[C,T](field, Sub(amount)))(tag)
		}
	}

	override def canApplyEffect(world: WorldView, entity: Entity): Boolean = true
}