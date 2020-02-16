package arx.ax4.game.entities

import arx.ax4.game.action.{CompoundSelectable, CompoundSelectableInstance, EntityPredicate, EntitySelector, Selectable, SelectableInstance, SelectionResult, Selector}
import arx.ax4.game.entities.Companions.CardData
import arx.ax4.game.entities.Conditionals.{CardInDeckConditional, EntityConditional}
import arx.ax4.game.entities.cardeffects.{AttackGameEffect, GameEffect, GameEffectConfigLoader, GameEffectInstance, PayActionPoints, PayStamina, SpecialAttackGameEffect}
import arx.ax4.game.logic.{CardLogic, SpecialAttackLogic}
import arx.core.NoAutoLoad
import arx.core.macros.GenerateCompanion
import arx.core.representation.ConfigValue
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.WorldView
import arx.graphics.helpers.{RichText, RichTextRenderSettings, THasRichTextRepresentation}
import arx.Prelude._
import arx.application.Noto

@GenerateCompanion
class DeckData extends AxAuxData {
	var drawPile : Vector[Entity] = Vector()
	var discardPile : Vector[Entity] = Vector()
	var hand : Vector[Entity] = Vector()
	var exhaustPile : Vector[Entity] = Vector()
	var attachedCards : Vector[Entity] = Vector() // cards that are attached to other cards and so cannot be drawn normally

	var lockedCards : Vector[LockedCard] = Vector()
	var lockedCardSlots : Vector[LockedCardSlot] = Vector()

	var drawCount : Int = 5

	def allNonExhaustedCards = drawPile ++ discardPile ++ hand ++ attachedCards
	def allAvailableCards = drawPile ++ discardPile ++ hand
	def allCards = drawPile ++ discardPile ++ hand ++ exhaustPile
	def containsCard(card : Entity) = drawPile.contains(card) || discardPile.contains(card) || hand.contains(card) || exhaustPile.contains(card)
}



case class LockedCardSlot(cardPredicates : Seq[EntityPredicate], description : String) extends Selectable {
	val cardSelector = EntitySelector(cardPredicates, description, this)

	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[SelectableInstance, String] = Left(new SelectableInstance {
		override def nextSelector(results: SelectionResult): Option[Selector[_]] = if (results.fullySatisfied(cardSelector)) { None } else { Some(cardSelector) }
	})
}
case class LockedCard(locked : LockedCardType, resolvedCard : Entity)
sealed trait LockedCardType
object LockedCardType {
	case object Empty extends LockedCardType
	case class SpecificCard(card : Entity) extends LockedCardType
	case class MetaAttackCard(key : AttackKey, specialAttack : Option[SpecialAttack]) extends LockedCardType
}


trait GameEffectModifier extends THasRichTextRepresentation {
	def modify(gameEffect : GameEffect) : PartialFunction[GameEffect,GameEffect]
	def modifyIfDefined(gameEffect : GameEffect) : GameEffect = {
		val f = modify(gameEffect)
		if (f.isDefinedAt(gameEffect)) {
			f.apply(gameEffect)
		} else {
			gameEffect
		}
	}
}
object GameEffectModifier {
	def applyAll(gameEffect : GameEffect, modifiers : Iterable[GameEffectModifier]) : GameEffect = modifiers.foldLeft(gameEffect)((a,b) => b.modifyIfDefined(a))
}
case class ApCostDeltaModifier(apDelta : Int, min : Option[Int]) extends GameEffectModifier {
	override def modify(gameEffect: GameEffect) = {
		case PayActionPoints(ap) => PayActionPoints((ap + apDelta).max(min.getOrElse(-1000)))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		val minStr = min.map(i => s" (min $i)").getOrElse("")
		RichText.parse(s"${apDelta.toSignedString} [ActionPoint]$minStr", settings)
	}
}
case class StaminaCostDeltaModifier(staminaDelta : Int, min : Option[Int]) extends GameEffectModifier {
	override def modify(gameEffect: GameEffect) = {
		case PayStamina(stamina) => PayStamina((stamina + staminaDelta).max(min.getOrElse(-1000)))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		val minStr = min.map(i => s" (min $i)").getOrElse("")
		RichText.parse(s"${staminaDelta.toSignedString} [StaminaPoint]$minStr", settings)
	}
}
case class AttackGameEffectModifier(modifier : AttackModifier) extends GameEffectModifier {
	override def modify(gameEffect: GameEffect): PartialFunction[GameEffect, GameEffect] = {
		case AttackGameEffect(key, attackData) => AttackGameEffect(key, attackData.mergedWith(modifier))
	}

	override def toRichText(settings: RichTextRenderSettings): RichText = {
		modifier.toRichText(settings)
	}
}


case class CardAttachment(condition : Vector[CardInDeckConditional], count : Int, automaticAttachment : Boolean, requiredForPlay : Boolean, removesFromDeck : Boolean, attachmentStyle: AttachmentStyle)

sealed trait AttachmentStyle
object AttachmentStyle {
	/** The attached card is effectively contained in the given card. It is (for practical purposes) removed from the deck as a whole
	 * and can only be accessed by means of this card */
	case object Contained extends AttachmentStyle
	/** The attached card will be played when this card is played, with the given set of modifiers applied to it */
	case class PlayModified(effectModifiers : Vector[GameEffectModifier]) extends AttachmentStyle
}

// Special attack is basically
// Play this other attack card, modified by:
// 	AP cost +/- X
//		Stamina cost +/- X
//		Other attack effects modified by Y
// Then apply other effects
//		Do additional damage
//		Lose HP
//		etc

@GenerateCompanion
class CardData extends AxAuxData {
	@NoAutoLoad
	var costs : Vector[GameEffect] = Vector()
	@NoAutoLoad
	var effects : Vector[GameEffect] = Vector()
	@NoAutoLoad
	var attachments : Map[AnyRef, CardAttachment] = Map()
	@NoAutoLoad
	var attachedCards : Map[AnyRef, Vector[Entity]] = Map()

	var attachedTo : Set[Entity] = Set()

	var cardType : Taxon = CardTypes.GenericCardType
	var name : String = "Card"
	var source : Entity = Entity.Sentinel
	var exhausted : Boolean = false

	@NoAutoLoad var xp : Map[Taxon, Int] = Map()

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		if (config.hasField("apCost")) {
			costs :+= PayActionPoints(config.apCost.int)
		}
		if (config.hasField("staminaCost")) {
			costs :+= PayStamina(config.staminaCost.int)
		}
		for (conf <- config.fieldAsList("costs")) {
			costs :+= GameEffectConfigLoader.loadFrom(conf)
		}
		for (conf <- config.fieldAsList("effects")) {
			effects :+= GameEffectConfigLoader.loadFrom(conf)
		}

		for (specialAttackConf <- config.fieldOpt("specialAttack")) {
			// a special attack card is set up such that it automatically attaches a single other attack card to itself automatically
			// when played it plays the base attack card, modified according to the terms of the special attack in addition to any other effects the card has
			val specialAttack = SpecialAttack.withName(specialAttackConf.str)
			effects :+= SpecialAttackGameEffect(specialAttack)
//			val attachStyle = AttachmentStyle.PlayModified(SpecialAttackLogic.specialAttackToEffectModifiers(specialAttack))
//			attachments += specialAttack -> CardAttachment(Vector(CardConditionals.CardMatchesSpecialAttack(specialAttack)), 1, automaticAttachment = true, requiredForPlay = true, removesFromDeck = false, attachStyle)
		}

		for (xpConf <- config.fieldOpt("xp")) {
			xp = if (xpConf.isObj) {
				xpConf.fields.map { case (k,v) => Taxonomy(k) -> v.int }
			} else {
				xpConf.str match {
					case CardDataRegex.xpRegex(skill, amount) => Map(Taxonomy(skill, "Skills") -> amount.toInt)
					case _ =>
						Noto.warn(s"unrecognized xp expression in card : $xpConf")
						Map()
				}
			}
		}
	}
}

object CardDataRegex {
	val xpRegex = "([a-zA-Z]+)\\s*->\\s*([0-9]+)".r
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
		val (effCosts, effEffects)  = CardLogic.effectiveCostsAndEffects(card)(view)
		effCosts ++ effEffects
	}

	override def instantiate(world: WorldView, entity: Entity, effectSource: Entity): Either[CardPlayInstance, String] = {
		implicit val view = world
		val CD = view.data[CardData](card)

		val (effCosts, effEffects)  = CardLogic.effectiveCostsAndEffects(card)

		// TODO: should the effect source be different for the ones that derive from attached cards?
		val costsRaw = effCosts.map(c => c -> c.instantiate(view, entity, effectSource))
		val effectsRaw = effEffects.map(e => e -> e.instantiate(view, entity, effectSource))

		for ((key,attachment) <- CD.attachments; attachedCards = CD.attachedCards.getOrElse(key, Vector())) {
			if (attachment.requiredForPlay && attachedCards.size < attachment.count) {
				return Right("Card must have other card(s) attached in order to be played")
			}
		}

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