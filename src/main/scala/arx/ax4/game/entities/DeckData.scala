package arx.ax4.game.entities

import arx.ax4.game.action.{CompoundSelectable, EntitySelector, Selectable}
import arx.ax4.game.entities.cardeffects.CardEffect
import arx.core.macros.GenerateCompanion
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView

@GenerateCompanion
class DeckData extends AxAuxData {
	var drawPile : Vector[Entity] = Vector()
	var discardPile : Vector[Entity] = Vector()
	var hand : Vector[Entity] = Vector()

	var drawCount : Int = 5
}

@GenerateCompanion
class CardData extends AxAuxData {
	var costs : Vector[CardEffect] = Vector()
	var effects : Vector[CardEffect] = Vector()
	var cardType : Taxon = CardTypes.GenericCardType
	var name : String = "Card"
}

object CardTypes {
	val GenericCardType = Taxonomy("CardType")
	val AttackCard = Taxonomy("AttackCard")
	val MoveCard = Taxonomy("MoveCard")
	val SkillCard = Taxonomy("SkillCard")
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

trait CardTrigger
object CardTrigger {
	case object OnDraw extends CardTrigger
	case object OnPlay extends CardTrigger
	case object OnDiscard extends CardTrigger
}

case class CardPlay(entity : Entity, card : Entity, view : WorldView) extends CompoundSelectable {
	val subParts = {
		val CD = view.data[CardData](card)
		CD.costs.map(_.instantiate(view, entity)) ++ CD.effects.map(_.instantiate(view, entity))
	}
	override def subSelectables(view : WorldView): Traversable[Selectable] = {
		subParts
	}
}