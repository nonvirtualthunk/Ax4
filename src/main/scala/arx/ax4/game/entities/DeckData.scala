package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.action.{DoNothingIntent, EntitySelector, GameActionIntent, GameActionIntentInstance, HexSelector, ResourceGatherSelector, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.{DeckData, Physical}
import arx.ax4.game.event.MovePointsGained
import arx.ax4.game.logic.{CardLogic, CharacterLogic, GatherLogic}
import arx.core.introspection.Field
import arx.core.introspection.FieldOperations.{Add, Sub}
import arx.core.macros.GenerateCompanion
import arx.core.vec.coordinates.{AxialVec3, HexRingIterator}
import arx.engine.data.TAuxData
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{FieldOperationModifier, World, WorldView}

import scala.reflect.ClassTag

@GenerateCompanion
class DeckData extends AxAuxData {
	var drawPile : Vector[Entity] = Vector()
	var discardPile : Vector[Entity] = Vector()
	var hand : Vector[Entity] = Vector()

	var drawCount : Int = 5
}


@GenerateCompanion
class CardData extends AxAuxData {
	var action : Option[GameActionIntent] = None
	var costs : Vector[Cost] = Vector()
	var cardType : Taxon = CardTypes.GenericCardType
}

object CardTypes {
	val GenericCardType = Taxonomy("CardType")
	val AttackCard = Taxonomy("AttackCard")
	val MoveCard = Taxonomy("MoveCard")
	val SkillCard = Taxonomy("SkillCard")
}


trait Cost {
	def selectors : List[Selector[_]] = Nil
	def pay(world : World, entity : Entity, selectionResult: SelectionResult)
	def canPay(world : WorldView, entity: Entity) : Boolean
}

case class ActionPointCost(ap : Int) extends Cost {
	override def pay(world : World, entity: Entity, selectionResult: SelectionResult): Unit = {
		CharacterLogic.useActionPoints(entity, ap)(world)
	}

	override def canPay(world: WorldView, entity: Entity): Boolean = {
		CharacterLogic.curActionPoints(entity)(world) >= ap
	}
}

object CardSelector {
	def apply(criteria : CardData => Boolean, description : String) : EntitySelector = {
		EntitySelector((view,entity) => {
			view.dataOpt[CardData](entity) match {
				case Some(cd) => criteria(cd)
				case None => false
			}
		}, description)
	}
}

case class DiscardCardsCost(numCards : Int, random : Boolean) extends Cost {
	val cardSelector = CardSelector(_ => true, "Card to discard").withAmount(numCards)
	override def selectors: List[Selector[_]] = List(cardSelector)

	override def pay(world: World, entity: Entity, selectionResult: SelectionResult): Unit = {
		world.dataOpt[DeckData](entity) match {
			case Some(_) =>
				val cards = selectionResult(cardSelector)
				CardLogic.discardCards(entity, cards, explicit = true)(world)
			case None =>
				Noto.error("No deck to discard from")
		}
	}

	override def canPay(world: WorldView, entity: Entity): Boolean = {
		world.dataOpt[DeckData](entity) match {
			case Some(deck) => deck.hand.size >= numCards
			case None => false
		}
	}
}

trait CardTrigger
object CardTrigger {
	case object OnDraw extends CardTrigger
	case object OnPlay extends CardTrigger
	case object OnDiscard extends CardTrigger
}

//trait CardEffect {
//	def instantiate(entity: Entity): Either[CardEffectInst, String]
//
//	def displayName(implicit view : WorldView) : String
//}
//
//trait CardEffectInst {
//	def nextSelector(world : WorldView, entity : Entity, results : SelectionResultBuilder) : Option[Selector[_]]
//	def applyEffect(world : World, entity : Entity, selectionResult: SelectionResult)
//	def canApplyEffect(world : WorldView, entity: Entity) : Boolean
//}

