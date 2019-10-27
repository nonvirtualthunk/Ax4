package arx.ax4.game.entities

import arx.ax4.game.entities.Conditionals.{AttackConditional, BaseAttackConditional}
import arx.ax4.game.entities.DamageType.{Piercing, Slashing}
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
	type AttackConditional = Conditional[AttackProspect]
	type BaseAttackConditional = Conditional[BaseAttackProspect]

	def all[T] : Conditional[T] = new Conditional[T] {
		override def isTrueFor(implicit view: WorldView, value: T): Boolean = true
	}

	//	case class AttackOfType
}

trait WorldConditional {
	def isTrueFor(view : WorldView) : Boolean
}


object AttackConditionals {

	case class WeaponIs()

	case object AnyAttackReference extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = true
	}

	abstract class BaseAttackConditionalHelper extends BaseAttackConditional {
		override def isTrueFor(implicit view: WorldView, attack: BaseAttackProspect): Boolean = {
			attack.attackReference.resolve().exists(AD => isTrueFor(view, AD))
		}
		def isTrueFor(implicit view: WorldView, attack: AttackData): Boolean
	}

	case class HasDamageType(damageType : DamageType) extends BaseAttackConditionalHelper {
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

	case object SingleTarget extends AttackConditional {
		override def isTrueFor(implicit view: WorldView, value: AttackProspect): Boolean = value.allTargets.size == 1
	}
}