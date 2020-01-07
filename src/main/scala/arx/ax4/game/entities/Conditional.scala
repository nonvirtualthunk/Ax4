package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.Conditionals.{BaseAttackConditional, CardInDeckConditional, EntityConditional}
import arx.ax4.game.entities.DamageType.{Piercing, Slashing}
import arx.ax4.game.entities.cardeffects.AttackGameEffect
import arx.ax4.game.logic.SpecialAttackLogic
import arx.core.representation.{ConfigValue, StringConfigValue}
import arx.engine.data.CustomConfigDataLoader
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.game.data.DicePoolBuilder._

trait Conditional[-T] {
	def source : String = ""

	private def outer = this
	def isTrueFor(implicit view : WorldView, value : T) : Boolean
	def and[U <: T](other : Conditional[U]) = new Conditional[U] {
		override def isTrueFor(implicit view: WorldView, value: U): Boolean = outer.isTrueFor(view, value) && other.isTrueFor(view, value) }
	def or[U <: T](other : Conditional[U]) = new Conditional[U] {
		override def isTrueFor(implicit view: WorldView, value: U): Boolean = outer.isTrueFor(view, value) || other.isTrueFor(view, value) }
}

object Conditionals {
//	type AttackConditional = Conditional[AttackProspect]
	type BaseAttackConditional = Conditional[BaseAttackProspect]
	type BaseGatherConditional = Conditional[BaseGatherProspect]
	type EntityConditional = Conditional[Entity]
	type CardInDeckConditional = Conditional[CardInDeck]

	private object AllConditional extends Conditional[Any] {
		override def isTrueFor(implicit view: WorldView, value: Any): Boolean = true
	}
	def all[T] : Conditional[T] = AllConditional.asInstanceOf[Conditional[T]]

	//	case class AttackOfType
}

case class CardInDeck(entity : Entity, card : Entity)

trait WorldConditional {
	def isTrueFor(view : WorldView) : Boolean
}

object EntityConditionals {
	case class isA(taxon : Taxon) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = value.dataOpt[IdentityData].exists(id => id.isA(taxon))
	}

	case class hasFlag(flag : Taxon, atLeastValue : Int) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = value.dataOpt[TagData].exists(td => td.flags.getOrElse(flag, -1) >= atLeastValue)
	}

	case object any extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = true
	}

	case class hasPerk(perkIdentity : Taxon) extends EntityConditional {
		override def isTrueFor(implicit view: WorldView, value: Entity): Boolean = {
			value.dataOpt[CharacterInfo].exists(ci => ci.perks.contains(perkIdentity))
		}
	}

	val PerkPattern = "(?i)Perk\\((.+)\\)".r
	val IsAPattern = "(?i)isA\\((.+)\\)".r
	val HasFlagPattern = "(?i)hasFlag\\((.+),([0-9]+)\\)".r
	def fromConfig(configValue : ConfigValue) : EntityConditional = {
		if (configValue.isStr) {
			configValue.str match {
				case PerkPattern(perkName) => hasPerk(Taxonomy(perkName))
				case IsAPattern(target) => isA(Taxonomy(target))
				case HasFlagPattern(flag, amount) => hasFlag(Taxonomy(flag), amount.toInt)
				case _ =>
					Noto.warn(s"unrecognized entity conditional string : ${configValue.str}")
					any
			}

		} else {
			Noto.warn("Non str config for entity conditional, cannot parse")
			any
		}
	}
}

object CardConditionals {
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
	}
}

object AttackConditionals extends CustomConfigDataLoader[BaseAttackConditional] {
	case class WeaponIs(isA : Taxon) extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = {
			value.attackData.weapon.dataOpt[IdentityData].exists(id => id.isA(isA))
		}
	}

	case object AnyAttack extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = true
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
			attack.damage.values.exists(de => de.damageType == damageType)
		}
	}

	case class HasTargetPattern(pattern: TargetPattern) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.targetPattern == pattern
	}

	case class HasAtLeastMinRange(minRange : Int) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.minRange >= minRange
	}
	case class HasAtLeastMaxRange(maxRange : Int) extends BaseAttackConditionalHelper {
		override def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean = attack.maxRange >= maxRange
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
	}



	val weaponIsPattern = "(?i)WeaponIsA\\((.+)\\)".r
	val anyAttackPattern = "(?i)Any".r
	val hasDamageTypePattern = "(?i)HasDamageType\\((.+)\\)".r
	val hasTargetPattern = "(?i)HasTargetPattern\\((.+)\\)".r

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[BaseAttackConditional]
	override def loadFrom(config: ConfigValue): BaseAttackConditional = if (config.isStr) {
		config.str match {
			case weaponIsPattern(weaponType) => WeaponIs(Taxonomy(weaponType))
			case anyAttackPattern() => AnyAttack
			case hasDamageTypePattern(damageType) => HasDamageType(Taxonomy(damageType))
			case hasTargetPattern(patternStr) => HasTargetPattern(TargetPattern.loadFrom(StringConfigValue(patternStr)))
			case _ =>
				Noto.warn(s"Invalid attack conditional string : ${config.str}")
				AnyAttack
		}
	} else if (config.isArr) {
		config.arr.map(c => loadFrom(c)).reduce((a,b) => a.and(b))
	} else if (config.isObj) {
		Noto.warn(s"Attack conditional object config not yet written : $config")
		AnyAttack
	} else {
		Noto.warn(s"Invalid configuration representation of attack conditional : $config")
		AnyAttack
	}
}