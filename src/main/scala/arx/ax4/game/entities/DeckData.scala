package arx.ax4.game.entities

import arx.ax4.game.action.{DoNothingIntent, GameActionIntent, SelectionResult, Selector}
import arx.ax4.game.logic.CharacterLogic
import arx.core.macros.GenerateCompanion
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}

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