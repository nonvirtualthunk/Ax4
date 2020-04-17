package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.Companions.DeckData
import arx.ax4.game.entities.Conditionals.{BaseAttackConditional, CardInDeckConditional, EntityConditional}
import arx.ax4.game.entities.DamageType.{Piercing, Slashing}
import arx.ax4.game.entities.cardeffects.AttackGameEffect
import arx.ax4.game.logic.{CardLocation, CardLogic, SpecialAttackLogic}
import arx.core.representation.{ConfigValue, StringConfigValue}
import arx.engine.data.CustomConfigDataLoader
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.game.data.DicePoolBuilder._
import arx.graphics.helpers.{RichText, RichTextRenderSettings, THasRichTextRepresentation, TextSection}

trait Conditional[-T] extends THasRichTextRepresentation {
	private def outer = this
	def isTrueFor(implicit view : WorldView, value : T) : Boolean
	def and[U <: T](other : Conditional[U]) = new Conditional[U] {
		override def isTrueFor(implicit view: WorldView, value: U): Boolean = outer.isTrueFor(view, value) && other.isTrueFor(view, value)

		override def toRichText(settings: RichTextRenderSettings): RichText = outer.toRichText(settings).append(" and ").append(other.toRichText(settings))
	}
	def or[U <: T](other : Conditional[U]) = new Conditional[U] {
		override def isTrueFor(implicit view: WorldView, value: U): Boolean = outer.isTrueFor(view, value) || other.isTrueFor(view, value)

		override def toRichText(settings: RichTextRenderSettings): RichText = outer.toRichText(settings).append(" or ").append(other.toRichText(settings))
	}
}

object Conditionals {
//	type AttackConditional = Conditional[AttackProspect]
	type BaseAttackConditional = Conditional[BaseAttackProspect]
	type BaseGatherConditional = Conditional[BaseGatherProspect]
	type EntityConditional = Conditional[Entity]
	type CardInDeckConditional = Conditional[CardInDeck]

	private object AllConditional extends Conditional[Any] {
		override def isTrueFor(implicit view: WorldView, value: Any): Boolean = true

		override def toRichText(settings: RichTextRenderSettings): RichText = "always"
	}
	def all[T] : Conditional[T] = AllConditional.asInstanceOf[Conditional[T]]

	//	case class AttackOfType
}

case class CardInDeck(entity : Entity, card : Entity)

trait WorldConditional {
	def isTrueFor(view : WorldView) : Boolean
}

object CardInDeckConditionals {
	case class CardMatchesSpecialAttack(specialAttack : SpecialAttack) extends CardInDeckConditional {
		override def isTrueFor(implicit view: WorldView, value: CardInDeck): Boolean = value.card.dataOpt[CardData] match {
			case Some(cardData) => cardData.effects.exists {
				case AttackGameEffect(_, attackData) => specialAttack.condition.isTrueFor(view, UntargetedAttackProspect(value.entity, attackData))
				case _ => false
			}
			case None =>
				Noto.warn("Card in deck conditional evaluating against card with no card data")
				false
		}

		override def toRichText(settings: RichTextRenderSettings): RichText =
			specialAttack.condition.toRichText(settings)
	}

}

object CardConditionals extends CustomConfigDataLoader[EntityConditional] {

	case class CardInLocation(location : CardLocation) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, card: Entity): Boolean =
			card.dataOpt[CardData].toVector
				.flatMap(cd => CardLogic.cardsInLocation(cd.inDeck, location))
				.contains(card)


		override def toRichText(settings: RichTextRenderSettings): RichText = s"Card is in $location"
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[EntityConditional]


	val cardInPattern = "(?i)card\\s?(is)?in\\s?\\(([a-zA-Z]+)\\)".r
	override def loadFrom(configValue: ConfigValue): Option[EntityConditional] = {
		configValue.str match {
			case cardInPattern(_, location) => CardLocation.parse(location).map(CardInLocation)
			case _ => None
		}
	}
}

object EntityConditionals extends CustomConfigDataLoader[EntityConditional] {
	case class isA(taxon : Taxon) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = value.dataOpt[IdentityData].exists(id => id.isA(taxon))

		override def toRichText(settings: RichTextRenderSettings): RichText = s"is a ${taxon.displayName}"
	}

	case class hasFlag(flag : Taxon, atLeastValue : Int) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = value.dataOpt[TagData].exists(td => td.flags.getOrElse(flag, -1) >= atLeastValue)

		override def toRichText(settings: RichTextRenderSettings): RichText = s"has ${flag.displayName} at least ${atLeastValue}"
	}

	case object any extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = true

		override def toRichText(settings: RichTextRenderSettings): RichText = s"always"
	}

	case class hasPerk(perkIdentity : Taxon) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = {
			value.dataOpt[CharacterInfo].exists(ci => ci.perks.contains(perkIdentity))
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = s"has perk ${perkIdentity.displayName}"
	}

	val PerkPattern = "(?i)Perk\\((.+)\\)".r
	val IsAPattern = "(?i)isA\\((.+)\\)".r
	val HasFlagPattern = "(?i)hasFlag\\((.+),([0-9]+)\\)".r

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[EntityConditional]

	override def loadFrom(topLevelConfig: ConfigValue): Option[EntityConditional] = {
		val subConditions = topLevelConfig.asList.flatMap(configValue =>
			(if (configValue.isStr) {
				configValue.str match {
					case PerkPattern(perkName) => Some(hasPerk(Taxonomy(perkName)))
					case IsAPattern(target) => Some(isA(Taxonomy(target)))
					case HasFlagPattern(flag, amount) => Some(hasFlag(Taxonomy(flag), amount.toInt))
					case _ =>
						None
				}

			} else {
				Noto.warn("Non str config for entity conditional, cannot parse")
				None
			}) : Option[EntityConditional]
		)
		if (subConditions.isEmpty) {
			None
		} else {
			Some(subConditions.reduceLeft(_ and _))
		}
	}
}


object AttackConditionals extends CustomConfigDataLoader[BaseAttackConditional] {
	case class WeaponIs(isA : Taxon) extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = {
			value.attackData.weapon.dataOpt[IdentityData].exists(id => id.isA(isA))
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"weapon is a ${isA.displayName}")
	}

	case class AttackTypeIs(isA : Taxon) extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = {
			value.attackData.attackType.isA(isA)
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"attack is a ${isA.displayName}")
	}

	case object AnyAttack extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = true

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"any attack")
	}

	abstract class BaseAttackConditionalHelper extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, attack: BaseAttackProspect): Boolean = {
			isTrueFor(view, attack.attackData)
		}
		def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean
	}

	case class HasDamageType(damageType : Taxon) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = {
			// true if the attack reference resolves to an attack where one of the kinds of damage it deals is the listed kind
			attack.damage.exists(de => de.damageType == damageType)
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText.parse(s"[$damageType] damage", settings)
	}

	case class HasTargetPattern(pattern: TargetPattern) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.targetPattern == pattern

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"targets ${pattern}")
	}

	case class HasAtLeastMinRange(minRange : Int) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.minRange >= minRange

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText.parse(s"${minRange} [MinimumRange]", settings)
	}
	case class HasAtLeastMaxRange(maxRange : Int) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.maxRange >= maxRange

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText.parse(s"$maxRange [MaximumRange]", settings)
	}

	abstract class AttackConditional extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = {
			value match {
				case ap : AttackProspect => isTrueForProspect(view, ap : AttackProspect)
				case _ => false
			}
		}
		def isTrueForProspect(implicit view: WorldView, value: AttackProspect): Boolean
	}

	case object SingleTarget extends AttackConditional {
		override def isTrueForProspect(implicit view: WorldView, value: AttackProspect): Boolean = value.allTargets.size == 1

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(s"attack has a single target")
	}

	case object IsUnarmedAttack extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = {
			value.attackData.weapon.dataOpt[Weapon].exists(_.naturalWeapon)
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = "unarmed attack"
	}



	val weaponIsPattern = "(?i)WeaponIsA?\\((.+)\\)".r
	val attackTypeIsPattern = "(?i)AttackIsA?\\((.+)\\)".r
	val anyAttackPattern = "(?i)Any".r
	val hasDamageTypePattern = "(?i)HasDamageType\\((.+)\\)".r
	val hasTargetPattern = "(?i)HasTargetPattern\\((.+)\\)".r
	val hasAtLeastMaxRange = "(?i)HasAtLeastMaxRange\\((\\d+)\\)".r
	val isUnarmed = "(?i)IsUnarmed(Attack)?".r

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[BaseAttackConditional]
	override def loadFrom(config: ConfigValue): Option[BaseAttackConditional] = if (config.isStr) {
		config.str match {
			case weaponIsPattern(weaponType) => Some(WeaponIs(Taxonomy(weaponType)))
			case attackTypeIsPattern(attackType) => Some(AttackTypeIs(Taxonomy(attackType, "AttackTypes")))
			case anyAttackPattern() => Some(AnyAttack)
			case hasDamageTypePattern(damageType) => Some(HasDamageType(Taxonomy(damageType)))
			case hasTargetPattern(patternStr) => Some(HasTargetPattern(TargetPattern.loadFromOrElse(StringConfigValue(patternStr), TargetPattern.SingleEnemy)))
			case hasAtLeastMaxRange(range) => Some(HasAtLeastMaxRange(range.toInt))
			case isUnarmed(_) => Some(IsUnarmedAttack)
			case _ =>
				Noto.warn(s"Invalid attack conditional string : ${config.str}")
				None
		}
	} else if (config.isArr) {
		Some(config.arr.flatMap(c => loadFrom(c)).reduce((a,b) => a.and(b)))
	} else if (config.isObj) {
		Noto.warn(s"Attack conditional object config not yet written : $config")
		None
	} else {
		Noto.warn(s"Invalid configuration representation of attack conditional : $config")
		None
	}
}