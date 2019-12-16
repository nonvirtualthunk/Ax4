package arx.ax4.game.entities

import arx.ax4.game.action.{CompoundSelectable, EntityPredicate, EntitySelector, Selectable}
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

	var lockedCards : Vector[LockedCardSlot] = Vector()
	var lockedCardSlots : Int = 2

	var drawCount : Int = 5

	def allAvailableCards = drawPile ++ discardPile ++ hand
	def allCards = drawPile ++ discardPile ++ hand ++ exhaustPile
	def containsCard(card : Entity) = drawPile.contains(card) || discardPile.contains(card) || hand.contains(card) || exhaustPile.contains(card)
}

case class LockedCardSlot(locked : LockedCard, resolvedCard : Entity)
sealed trait LockedCard
case class LockedSpecificCard(card : Entity) extends LockedCard
case class LockedMetaAttackCard(key : AttackKey, specialSource : Option[Entity], specialKey : AnyRef) extends LockedCard

@GenerateCompanion
class CardData extends AxAuxData {
	var costs : Vector[CardEffect] = Vector()
	var effects : Vector[CardEffect] = Vector()
	var cardType : Taxon = CardTypes.GenericCardType
	var name : String = "Card"
	var source : Entity = Entity.Sentinel
}

object CardTypes {
	val GenericCardType = Taxonomy("CardType")
	val AttackCard = Taxonomy("AttackCard")
	val MoveCard = Taxonomy("MoveCard")
	val SkillCard = Taxonomy("SkillCard")
	val ItemCard = Taxonomy("ItemCard")
}



object CardSelector {
	def AnyCard(desc : String) = new EntitySelector(Seq(CardPredicate.IsCard), "Any Card")
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