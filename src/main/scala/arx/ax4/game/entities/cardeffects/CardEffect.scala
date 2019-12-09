package arx.ax4.game.entities.cardeffects

import arx.application.Noto
import arx.ax4.game.action.{HexSelector, ResourceGatherSelector, Selectable, SelectionResult, Selector}
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
	def instantiate (implicit view : WorldView, entity : Entity) : CardEffectInst
}

trait CardEffectInst extends Selectable {

	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = nextSelector(results)

	def nextSelector(results : SelectionResult) : Option[Selector[_]]
	def applyEffect(selectionResult: SelectionResult, world: World): Unit
	def canApplyEffect: Boolean
}

case class Gather(range : Int) extends CardEffect {

	override def instantiate(implicit view: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {

		def hexSelector(entity: Entity) = HexSelector(TargetPattern.Point, (view, v) => {
			v.distance(entity(Physical)(view).position) <= range && GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v))(view).nonEmpty
		})

		def prospectSelector(view: WorldView, entity: Entity, target: AxialVec3) = ResourceGatherSelector(GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(target))(view))


		override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
			val hexSel = hexSelector(entity)
			if (!results.fullySatisfied(hexSel)) {
				Some(hexSel)
			} else {
				val hex = results.single(hexSel)
				val rsrcSel = prospectSelector(view, entity, hex)
				if (results.fullySatisfied(rsrcSel)) {
					None
				} else {
					Some(rsrcSel)
				}
			}
		}

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			val hex = selectionResult.single(hexSelector(entity))
			val prospect = selectionResult.single(prospectSelector(view, entity, hex))

			prospect.toGatherProspect(view) match {
				case Some(p) => GatherLogic.gather(p)(world)
				case None => Noto.error(s"Gather failed after being selected: $prospect")
			}
		}

		override def canApplyEffect: Boolean = {
			val center = entity(Physical).position
			(0 to range).exists { r => HexRingIterator(center, r).exists(v => {
				GatherLogic.gatherProspectsFor(entity, Tiles.tileAt(v)).nonEmpty
			})}
		}
	}
}

case class GainMove(mp : Int) extends CardEffect {

	override def instantiate(implicit view: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = None

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			CharacterLogic.gainMovePoints(entity, mp)(world)
		}

		override def canApplyEffect: Boolean = true
	}
}

case class PayActionPoints(ap : Int) extends CardEffect {
	override def instantiate(implicit view: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = None

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			CharacterLogic.useActionPoints(entity, ap)(world)
		}

		override def canApplyEffect: Boolean = CharacterLogic.curActionPoints(entity)(view) >= ap
	}
}


case class DiscardCards(numCards : Int, random : Boolean) extends CardEffect {
	override def instantiate(implicit world: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {
		val cardSelector = CardSelector(_ => true, "Card to discard").withAmount(numCards)

		override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) {
			None
		} else {
			Some(cardSelector)
		}

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			world.view.dataOpt[DeckData](entity) match {
				case Some(_) =>
					val cards = selectionResult(cardSelector)
					CardLogic.discardCards(entity, cards, explicit = true)(world)
				case None =>
					Noto.error("No deck to discard from")
			}
		}

		override def canApplyEffect: Boolean = {
			world.dataOpt[DeckData](entity) match {
				case Some(deck) => deck.hand.size >= numCards
				case None => false
			}
		}
	}
}

//case class IncreaseFlag(flag : Taxon, n : Int) extends CardEffect
//case class DecreaseFlag(flag : Taxon, n : Int) extends CardEffect

case class AddValue[C <: TAuxData, T : Numeric](field : Field[C,T], amount : T, to : Selector[Entity])(implicit tag : ClassTag[C]) extends CardEffect {
	override def instantiate(implicit world: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
			if (results.fullySatisfied(to)) {
				None
			} else {
				Some(to)
			}
		}

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			for (target <- selectionResult(to)) {
				world.modify[C](target, FieldOperationModifier[C, T](field, Add(amount)))(tag)
			}
		}

		override def canApplyEffect: Boolean = true
	}
}

case class SubValue[C <: TAuxData, T : Numeric](field : Field[C,T], amount : T, to : Selector[Entity])(implicit tag : ClassTag[C]) extends CardEffect {
	override def instantiate(implicit world: WorldView, entity: Entity): CardEffectInst = new CardEffectInst {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = {
			if (results.fullySatisfied(to)) {
				None
			} else {
				Some(to)
			}
		}

		override def applyEffect(selectionResult: SelectionResult, world: World): Unit = {
			for (target <- selectionResult(to)) {
				world.modify[C](target, FieldOperationModifier[C, T](field, Sub(amount)))(tag)
			}
		}

		override def canApplyEffect: Boolean = true
	}
}

