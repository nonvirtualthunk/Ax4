package arx.ax4.game.entities

import arx.ax4.game.action.{CompoundSelectable, CompoundSelectableInstance, EntityPredicate, EntitySelector, Selectable, SelectableInstance, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.CardData
import arx.ax4.game.entities.cardeffects.{GameEffect, CardEffectConfigLoader, GameEffectInstance, PayActionPoints, PayStamina}
import arx.core.NoAutoLoad
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
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

	override def instantiate(world: WorldView, entity: Entity): Either[SelectableInstance, String] = Left(new SelectableInstance {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) { None } else { Some(cardSelector) }
	})
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
	@NoAutoLoad
	var costs : Vector[GameEffect] = Vector()
	@NoAutoLoad
	var effects : Vector[GameEffect] = Vector()
	var cardType : Taxon = CardTypes.GenericCardType
	var name : String = "Card"
	var source : Entity = Entity.Sentinel
	var exhausted : Boolean = false

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		if (config.hasField("apCost")) {
			costs :+= PayActionPoints(config.apCost.int)
		}
		if (config.hasField("staminaCost")) {
			costs :+= PayStamina(config.staminaCost.int)
		}
		for (costsConf <- config.fieldOpt("costs") ; conf <- costsConf.arr) {
			costs :+= CardEffectConfigLoader.loadFrom(conf)
		}
		for (effectsConf <- config.fieldOpt("effects") ; conf <- effectsConf.arr) {
			effects :+= CardEffectConfigLoader.loadFrom(conf)
		}
	}
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

	override def instantiate(world: WorldView, entity: Entity): Either[CardPlayInstance, String] = {
		implicit val view = world
		val CD = view.data[CardData](card)
		val costsRaw = CD.costs.map(c => c -> c.instantiate(view, entity))
		val effectsRaw = CD.effects.map(e => e -> e.instantiate(view, entity))

		(costsRaw.map(_._2).collect { case Right(msg) => msg } ++ effectsRaw.map(_._2).collect { case Right(msg) => msg }).headOption match {
			case Some(msg) => Right(msg)
			case _ =>
				val costs = costsRaw.collect { case (k, Left(value)) => k -> value }
				val effects = effectsRaw.collect { case (k, Left(value)) => k -> value }

				Left(CardPlayInstance(costs, effects))
		}
	}
}

case class CardPlayInstance(costs : Vector[(GameEffect, GameEffectInstance)], effects : Vector[(GameEffect, GameEffectInstance)]) extends CompoundSelectableInstance(costs ++ effects) {

}

object CardPredicate {
	case object IsCard extends EntityPredicate {
		override def matches(view: WorldView, entity: Entity): Boolean = entity.hasData(CardData)(view)
	}
}