package arx.ax4.game.entities

import arx.ax4.game.action.{CompoundSelectable, EntityPredicate, EntitySelector, Selectable, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.CardData
import arx.ax4.game.entities.cardeffects.CardEffect
import arx.core.macros.GenerateCompanion
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView

@GenerateCompanion
class DeckData extends AxAuxData {
	var drawPile : Vector[Entity] = Vector()
	var discardPile : Vector[Entity] = Vector()
	var hand : Vector[Entity] = Vector()
	var exhaustPile : Vector[Entity] = Vector()

	var lockedCards : Vector[LockedCard] = Vector()
	var lockedCardSlots : Vector[LockedCardSlot] = Vector()

	var drawCount : Int = 5

	def allAvailableCards = drawPile ++ discardPile ++ hand
	def allCards = drawPile ++ discardPile ++ hand ++ exhaustPile
	def containsCard(card : Entity) = drawPile.contains(card) || discardPile.contains(card) || hand.contains(card) || exhaustPile.contains(card)
}



case class LockedCardSlot(cardPredicates : Seq[EntityPredicate], description : String) extends Selectable {
	val cardSelector = EntitySelector(cardPredicates, description, this)
	override def nextSelector(world: WorldView, entity: Entity, results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) { None } else { Some(cardSelector) }
}
case class LockedCard(locked : LockedCardType, resolvedCard : Entity)
sealed trait LockedCardType
object LockedCardType {
	case object Empty extends LockedCardType
	case class SpecificCard(card : Entity) extends LockedCardType
	case class MetaAttackCard(key : AttackKey, specialSource : Option[Entity], specialKey : AnyRef) extends LockedCardType
}

@GenerateCompanion
class CardData extends AxAuxData {
	var costs : Vector[CardEffect] = Vector()
	var effects : Vector[CardEffect] = Vector()
	var cardType : Taxon = CardTypes.GenericCardType
	var name : String = "Card"
	var source : Entity = Entity.Sentinel
	var exhausted : Boolean = false
}

object CardTypes {
	val GenericCardType = Taxonomy("CardType")
	val AttackCard = Taxonomy("AttackCard")
	val MoveCard = Taxonomy("MoveCard")
	val SkillCard = Taxonomy("SkillCard")
	val GatherCard = Taxonomy("GatherCard")
	val ItemCard = Taxonomy("ItemCard")
}



object CardSelector {
	def AnyCard(desc : String, selectable : Selectable) = new EntitySelector(Seq(CardPredicate.IsCard), "Any Card", selectable)
}


trait CardTrigger
object CardTrigger {
	case object OnDraw extends CardTrigger
	case object OnPlay extends CardTrigger
	case object OnDiscard extends CardTrigger
}

case class CardPlay(card : Entity) extends CompoundSelectable {
	override def subSelectables(view : WorldView): Traversable[Selectable] = {
		val CD = view.data[CardData](card)
		CD.costs ++ CD.effects
	}
}

object CardPredicate {
	case object IsCard extends EntityPredicate {
		override def matches(view: WorldView, entity: Entity): Boolean = entity.hasData(CardData)(view)
	}
}