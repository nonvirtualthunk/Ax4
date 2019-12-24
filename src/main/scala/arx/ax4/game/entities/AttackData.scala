package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.AttackConditionals.AnyAttackReference
import arx.ax4.game.entities.Conditionals.BaseAttackConditional
import arx.ax4.game.entities.DamageType.{Piercing, Unknown}
import arx.ax4.game.entities.TargetPattern.Point
import arx.ax4.game.entities.cardeffects.{DrawCards, GameEffect}
import arx.ax4.game.logic.AllegianceLogic
import arx.core.introspection.{CopyAssistant, ReflectionAssistant, TEagerSingleton}
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.{AxialVec, AxialVec3, CartVec}
import arx.engine.data.{ConfigLoadable, TNestedData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{Breakdown, WorldView}
import arx.game.data.DicePool


case class AttackData(var name: String = "Strike",
							 var accuracyBonus: Int = 0,
							 var strikeCount: Int = 1,
							 var staminaCost: Int = 1,
							 var actionCost: Int = 1,
							 var minRange: Int = 0,
							 var maxRange: Int = 1,
							 var damage: Map[AnyRef, DamageElement] = Map(),
							 var targetPattern: TargetPattern = TargetPattern.SingleEnemy,
							 var cardCount : Int = 3
							) extends ConfigLoadable {
	def merge(modifiers: AttackModifier): Unit = {
		name = modifiers.namePrefix.getOrElse("") + name
		name = modifiers.nameOverride.getOrElse(name)
		accuracyBonus += modifiers.accuracyBonus
		strikeCount += modifiers.strikeCountBonus
		staminaCost += modifiers.staminaCostDelta
		actionCost = (actionCost + modifiers.actionCostDelta).max(modifiers.actionCostMinimum)
		minRange = modifiers.minRangeOverride.getOrElse(minRange)
		minRange = minRange + modifiers.minRangeDelta.getOrElse(0)
		maxRange = modifiers.maxRangeOverride.getOrElse(maxRange)
		maxRange = maxRange + modifiers.maxRangeDelta.getOrElse(0)
		targetPattern = modifiers.targetPatternOverride.getOrElse(targetPattern)
		for ((src, dmg) <- modifiers.damageBonuses) {
			val existing = damage.get(src)
			val newDmg = existing match {
				case Some(ext) => {
					val dice = dmg.damageDice.map(d => ext.damageDice.mergedWith(d)).getOrElse(ext.damageDice)
					val bonus = dmg.damageBonus.map(b => ext.damageBonus + b).getOrElse(ext.damageBonus)
					val mult = dmg.damageMultiplier.map(d => 1.0f + (ext.damageMultiplier - 1.0f) + (d - 1.0f)).getOrElse(ext.damageMultiplier)
					val dmgType = dmg.damageType.getOrElse(ext.damageType)
					DamageElement(dice, bonus, mult, dmgType)
				}
				case None =>
					Noto.warn("Modifier to damage element but no base damage element present")
					DamageElement(dmg.damageDice.getOrElse(DicePool.none), dmg.damageBonus.getOrElse(0), dmg.damageMultiplier.getOrElse(1.0f), dmg.damageType.getOrElse(Unknown))
			}
			damage += src -> newDmg
		}
	}

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (dmgConfig <- config.fieldOpt("damage")) {
			if (dmgConfig.isObj) {
				for ((dmgSrc, dmgDataConf) <- dmgConfig.fields) {
					DamageElement.parse(dmgDataConf.str) match {
						case Some(dmgElem) => damage += dmgSrc -> dmgElem
						case _ => Noto.error(s"malformed damage element : ${dmgDataConf.str}")
					}
				}
			} else {
				DamageElement.parse(dmgConfig.str) match {
					case Some(dmgElem) => damage += AttackData.PrimaryDamageKey -> dmgElem
					case None => Noto.error(s"malformed top level damage element : ${dmgConfig.str}")
				}
			}
		}
		// TODO : target pattern
	}
}

object AttackData {
	val PrimaryDamageKey = "primary"
}

case class DefenseData(var armor: Int = 0,
							  var dodgeBonus: Int = 0) {
	def merge(modifiers: DefenseModifier): Unit = {
		armor += modifiers.armorBonus
		dodgeBonus += modifiers.dodgeBonus
	}
}


@GenerateCompanion
case class AttackModifier(var nameOverride: Option[String] = None,
								  var namePrefix: Option[String] = None,
								  var accuracyBonus: Int = 0,
								  var strikeCountBonus: Int = 0,
								  var actionCostDelta : Int = 0,
								  var actionCostMinimum : Int = 0,
								  var staminaCostDelta: Int = 0,
								  var minRangeDelta : Option[Int] = None,
								  var minRangeOverride: Option[Int] = None,
								  var maxRangeDelta : Option[Int] = None,
								  var maxRangeOverride: Option[Int] = None,
								  var damageBonuses: Map[AnyRef, DamageElementDelta] = Map(),
								  var targetPatternOverride: Option[TargetPattern] = None) extends TNestedData

@GenerateCompanion
case class DefenseModifier(var dodgeBonus: Int = 0,
									var armorBonus: Int = 0) extends TNestedData

class SpecialAttack {
	var condition: BaseAttackConditional = AnyAttackReference
	var attackModifier: AttackModifier = AttackModifier()
	var additionalEffects : Seq[GameEffect] = Nil
}

object SpecialAttack {


	case object PiercingStab extends SpecialAttack {
		condition = (AttackConditionals.HasTargetPattern(Point) or AttackConditionals.HasTargetPattern(TargetPattern.SingleEnemy)) and AttackConditionals.HasDamageType(Piercing) and AttackConditionals.HasAtLeastMaxRange(2)
		attackModifier = AttackModifier(
			nameOverride = Some("piercing stab"),
			accuracyBonus = -1,
			targetPatternOverride = Some(TargetPattern.Line(startDist = 1, length = 2)),
			minRangeOverride = Some(1),
			maxRangeOverride = Some(1)
		)
	}

	case object PowerAttack extends SpecialAttack {
		condition = AttackConditionals.AnyAttackReference
		attackModifier = AttackModifier(
			namePrefix = Some("power attack : "),
			accuracyBonus = -2,
			damageBonuses = Map(AttackData.PrimaryDamageKey -> DamageElementDelta(damageMultiplier = Some(2.0f)))
		)
	}

	case object SwiftStab extends SpecialAttack {
		condition = AttackConditionals.HasDamageType(Piercing)
		attackModifier = AttackModifier(
			nameOverride = Some("swift stab"),
			accuracyBonus = -1,
			damageBonuses = Map(AttackData.PrimaryDamageKey -> DamageElementDelta(damageBonus = Some(-1))),
			actionCostDelta = -1,
			actionCostMinimum = 1
		)
		additionalEffects = Seq(
			DrawCards(1)
		)
	}

	import arx.Prelude._
	private lazy val byNameMap = ReflectionAssistant.instancesOfSubtypesOf[SpecialAttack].map(inst => inst.getClass.getSimpleName.replace("$","").toLowerCase.stripWhitespace -> inst).toMap
	def withNameExists(name : String) = byNameMap.contains(name.toLowerCase().stripWhitespace)
	def withName(name : String) = byNameMap(name.toLowerCase().stripWhitespace)
}


object DamageType extends TEagerSingleton {
	val Physical = Taxonomy("DamageType")

	val Bludgeoning = Taxonomy("DamageTypes.Bludgeoning")

	val Piercing = Taxonomy("DamageTypes.Piercing")

	val Slashing = Taxonomy("DamageTypes.Slashing")

	val Unknown = Taxonomy("DamageTypes.Unknown")
}

case class DamageElement(damageDice: DicePool, damageBonus: Int, damageMultiplier: Float, damageType: Taxon) {
}

case class DamageElementDelta(damageDice: Option[DicePool] = None, damageBonus: Option[Int] = None, damageMultiplier: Option[Float] = None, damageType: Option[Taxon] = None)

object DamageElementDelta {
	def damageBonus(n: Int) = DamageElementDelta(damageBonus = Some(n))
}

object DamageElement {
	val damageRegex = "([0-9]+)\\s*d\\s*([0-9]+)\\s*([+-]\\s*[0-9]+)?(.*)?".r
	val bonusRegex = "([+-])\\s*([0-9]+)".r

	def toString(elements: Traversable[DamageElement]) = {
		elements.map(e => e.damageDice.toString)
	}

	def parse(str: String): Option[DamageElement] = {
		str match {
			case damageRegex(dieCountStr, pipsStr, nullableBonusStr, nullableDamageTypeStr) =>
				val bonus = Option(nullableBonusStr) collect {
					case bonusRegex(sign, amount) => sign match {
						case "+" => amount.toInt
						case "-" => -amount.toInt
					}
				}
				val damageType = Option(nullableDamageTypeStr).map(str => Taxonomy(str.toLowerCase())).getOrElse(DamageType.Unknown)
				val dieCount = dieCountStr.toInt
				val pips = pipsStr.toInt

				Some(DamageElement(DicePool(dieCount).d(pips), bonus.getOrElse(0), 1.0f, damageType))
			case _ => None
		}
	}
}

trait BaseAttackProspect {
	def attacker: Entity

	def attackReference: AttackReference
}

case class UntargetedAttackProspect(attacker: Entity, attackReference: AttackReference) extends BaseAttackProspect

case class AttackProspect(attacker: Entity, attackReference: AttackReference, target: Entity, allTargets: Seq[Entity], attackData: AttackData, defenseData: DefenseData) extends BaseAttackProspect


//case class AttackResult(attacker: Entity, defender: Entity, strikes: List[StrikeResult])
//
//case class StrikeResult(outcomes: Set[StrikeOutcome], damage: List[DamageResult])

case class DamageResult(amount: Int, damageType: Taxon)

trait StrikeOutcome

object StrikeOutcome {

	case object Miss extends StrikeOutcome

	case object Hit extends StrikeOutcome

	case object Dodged extends StrikeOutcome

	case object Armored extends StrikeOutcome

	case object Blocked extends StrikeOutcome

}


sealed trait TargetPattern {

}

trait EntityTarget extends TargetPattern {
	def matches(view: WorldView, attacker: Entity, target: Entity): Boolean

	def count: Int
}

trait HexTargetPattern extends TargetPattern {
	def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3]
}

object TargetPattern {

	case class Enemies(count: Int) extends EntityTarget {
		override def matches(view: WorldView, attacker: Entity, target: Entity): Boolean = AllegianceLogic.areEnemies(attacker, target)(view)
	}

	def SingleEnemy = Enemies(1)

	case object Point extends HexTargetPattern {
		override def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3] = targetPoint :: Nil
	}

	case class Line(startDist: Int, length: Int) extends HexTargetPattern {
		override def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3] = {
			//			val sourceCart = sourcePoint.qr.asCartesian
			//			val delta = CartVec((targetPoint.qr.asCartesian - sourceCart).normalizeSafe)
			//			(startDist until (startDist + length)).map(i => AxialVec.fromCartesian(sourceCart + delta * i.toFloat)).map(ax => AxialVec3(ax.q, ax.r, sourcePoint.l))
			val q = sourcePoint.sideClosestTo(targetPoint, AxialVec3.Zero)
			(startDist until (startDist + length)).map(i => AxialVec3(sourcePoint.plusDir(q, i), sourcePoint.l))
		}
	}

}