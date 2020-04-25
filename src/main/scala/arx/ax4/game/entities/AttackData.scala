package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.AffectsSelector.Target
import arx.ax4.game.entities.AttackConditionals.AnyAttack
import arx.ax4.game.entities.AttackEffectTrigger.Hit
import arx.ax4.game.entities.Conditionals.BaseAttackConditional
import arx.ax4.game.entities.DamageKey.Primary
import arx.ax4.game.entities.DamageType.{Piercing, Unknown}
import arx.ax4.game.entities.TargetPattern.Point
import arx.ax4.game.entities.cardeffects.{DrawCards, GameEffect, GameEffectConfigLoader}
import arx.ax4.game.logic.AllegianceLogic
import arx.core.NoAutoLoad
import arx.core.introspection.{CopyAssistant, ReflectionAssistant, TEagerSingleton}
import arx.core.macros.GenerateCompanion
import arx.core.math.Sext
import arx.core.representation.ConfigValue
import arx.core.vec.coordinates.{AxialVec, AxialVec3, CartVec}
import arx.engine.data.{ConfigLoadable, CustomConfigDataLoader, TNestedData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{Breakdown, WorldView}
import arx.game.data.DicePool
import arx.graphics.helpers.{HorizontalPaddingSection, ImageSection, LineBreakSection, RGBA, RichText, RichTextRenderSettings, RichTextScale, RichTextSection, THasRichTextRepresentation, TaxonSections, TextSection}
import com.typesafe.config.Config


case class AttackData(var weapon: Entity,
											var name: String = "Strike",
											var attackType: Taxon = Taxonomy("melee attack"),
											var accuracyBonus: Int = 0,
											var strikeCount: Int = 1,
											var staminaCost: Int = 1,
											var actionCost: Int = 1,
											var minRange: Int = 0,
											var maxRange: Int = 1,
											var damage: Vector[DamageElement] = Vector(),
											var targetPattern: TargetPattern = TargetPattern.SingleEnemy,
											var cardCount: Int = 3,
											var triggeredEffects: Vector[TriggeredAttackEffect] = Vector(),
							) extends ConfigLoadable {
	def merge(modifiers: AttackModifier): Unit = {
		name = modifiers.namePrefix.getOrElse("") + name
		name = modifiers.nameOverride.getOrElse(name)
		accuracyBonus += modifiers.accuracyBonus
		strikeCount *= modifiers.strikeCountMultiplier
		strikeCount += modifiers.strikeCountBonus
		staminaCost = (staminaCost + modifiers.staminaCostDelta).max(modifiers.staminaCostMinimum.getOrElse(-1000))
		actionCost = (actionCost + modifiers.actionCostDelta).max(modifiers.actionCostMinimum.getOrElse(-1000))
		minRange = modifiers.minRangeOverride.getOrElse(minRange)
		minRange = minRange + modifiers.minRangeDelta.getOrElse(0)
		maxRange = modifiers.maxRangeOverride.getOrElse(maxRange)
		maxRange = maxRange + modifiers.maxRangeDelta.getOrElse(0)
		targetPattern = modifiers.targetPatternOverride.getOrElse(targetPattern)
		triggeredEffects ++= modifiers.triggeredEffects
		modifiers.damageModifiers.foreach {
			case DamageModifier(predicate, delta) =>
				damage = damage.map(de => if (predicate.matches(de)) {
					delta.modify(de)
				} else {
					de
				})
		}
	}

	def mergedWith(modifiers: AttackModifier): AttackData = {
		val ret = copy()
		ret.merge(modifiers)
		ret
	}

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		// custom in order to support either singular or plural damage
		//		damage = config.fieldAsList("damage").map(DamageElement.loadFrom).toVector
		// TODO : target pattern

		for (hitTargetEffect <- config.fieldAsList("onHitTargetEffects")) {
			GameEffectConfigLoader.loadFrom(hitTargetEffect) match {
				case Some(effect) => triggeredEffects :+= TriggeredAttackEffect(AttackEffectTrigger.Hit, AffectsSelector.Target, effect)
				case None => Noto.error(s"Invalid effect in onHitTargetEffects: $hitTargetEffect")
			}
		}

		for (hitSelfEffect <- config.fieldAsList("onHitSelfEffects")) {
			GameEffectConfigLoader.loadFrom(hitSelfEffect) match {
				case Some(effect) => triggeredEffects :+= TriggeredAttackEffect(AttackEffectTrigger.Hit, AffectsSelector.Self, effect)
				case None => Noto.error(s"Invalid effect in onHitSelfEffects: $hitSelfEffect")
			}
		}
	}
}

case class TriggeredAttackEffect(var trigger : AttackEffectTrigger = Hit, var affects : AffectsSelector = Target, var effect : GameEffect = GameEffect.Sentinel) extends ConfigLoadable
object TriggeredAttackEffect extends CustomConfigDataLoader[TriggeredAttackEffect] {
	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[TriggeredAttackEffect]

	override def loadFrom(config: ConfigValue): Option[TriggeredAttackEffect] = {
		val base = TriggeredAttackEffect()
		base.loadFromConfig(config)
		Some(base)
	}
}


sealed abstract class AttackEffectTrigger(val name : String) {
	override def toString: String = name
}
object AttackEffectTrigger extends  CustomConfigDataLoader[AttackEffectTrigger] {
	case object Hit extends AttackEffectTrigger("hit")

	case object Miss extends AttackEffectTrigger("miss")

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[AttackEffectTrigger]

	override def loadFrom(config: ConfigValue): Option[AttackEffectTrigger] = config.str.toLowerCase() match {
		case "hit" => Some(Hit)
		case "miss" => Some(Miss)
		case _ => None
	}
}

sealed trait AffectsSelector
object AffectsSelector extends CustomConfigDataLoader[AffectsSelector] {
	case object Self extends AffectsSelector

	case object Target extends AffectsSelector

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[AffectsSelector]

	override def loadFrom(config: ConfigValue): Option[AffectsSelector] = config.str.toLowerCase() match {
		case "self" => Some(Self)
		case "target" => Some(Target)
		case _ => None
	}
}


trait DamageKey extends ConfigLoadable

object DamageKey extends CustomConfigDataLoader[DamageKey] {

	case object Primary extends DamageKey {
		override def toString: String = "primary"
	}

	case class Custom(key: String) extends DamageKey {
		override def toString: String = key
	}

	def parse(str: String): DamageKey = {
		str.toLowerCase match {
			case "primary" => Primary
			case other => Custom(other)
		}
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[DamageKey]

	override def loadFrom(config: ConfigValue): Option[DamageKey] = Some(parse(config.str))
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
													var strikeCountMultiplier: Int = 1,
													var actionCostDelta: Int = 0,
													var actionCostMinimum: Option[Int] = None,
													var staminaCostDelta: Int = 0,
													var staminaCostMultiplier : Int = 1,
													var staminaCostMinimum: Option[Int] = None,
													var minRangeDelta: Option[Int] = None,
													var minRangeOverride: Option[Int] = None,
													var maxRangeDelta: Option[Int] = None,
													var maxRangeOverride: Option[Int] = None,
													var damageModifiers: Vector[DamageModifier] = Vector(),
													var baseDamageOverride: Option[DamageElement] = None,
													var targetPatternOverride: Option[TargetPattern] = None,
													var triggeredEffects: Vector[TriggeredAttackEffect] = Vector()) extends TNestedData with ConfigLoadable with THasRichTextRepresentation {
	override def toRichText(settings: RichTextRenderSettings): RichText = {
		import arx.Prelude._

		var res = RichText.Empty

		def appendConceptSection(concept: String, amount: Int, noOpAmount : Int = 0, prefixNumber : String = "") = {
			if (amount != noOpAmount) {
				if (prefixNumber.isEmpty) {
					res += TextSection(amount.toSignedString)
				} else {
					res += TextSection(prefixNumber + amount)
				}
				res ++= TaxonSections(Taxonomy(concept), settings)
				true
			} else {
				false
			}
		}

		var anyAdded = false
		anyAdded |= appendConceptSection("GameConcepts.Accuracy", accuracyBonus)
		anyAdded |= appendConceptSection("GameConcepts.ActionPoint", actionCostDelta)
		anyAdded |= appendConceptSection("GameConcepts.StaminaPoint", staminaCostDelta)
		anyAdded |= appendConceptSection("GameConcepts.StaminaPoint", staminaCostMultiplier, 1, "x")
		anyAdded |= appendConceptSection("GameConcepts.Strike", strikeCountMultiplier, 1, "x")

		for (minR <- minRangeDelta) {
			if (res.nonEmpty) { res += LineBreakSection(0) }
			appendConceptSection("GameConcepts.MinimumRange", minR)
		}
		for (maxR <- maxRangeDelta) {
			if (res.nonEmpty) { res += LineBreakSection(0) }
			appendConceptSection("GameConcepts.MaximumRange", maxR)
		}
		if (damageModifiers.nonEmpty) {
			if (res.nonEmpty) { res += LineBreakSection(0) }
			res ++= damageModifiers.flatMap(_.delta.toRichText(settings).sections)
		}
		for (dmg <- baseDamageOverride) {
			if (res.nonEmpty) { res += LineBreakSection(0) }
			res ++= dmg.toRichText(settings)
		}

		targetPatternOverride match {
			case Some(pattern: HexTargetPattern) =>
				if (res.nonEmpty) { res += LineBreakSection(0) }
				res ++= Vector(TextSection("Target"), HorizontalPaddingSection(4))
				res ++= pattern.toRichText(settings)
			case _ => // do nothing
		}


		if (res.nonEmpty) { res += LineBreakSection(0) }
		res ++= AttackData.renderTriggeredAttackEffects(triggeredEffects, settings)
		// TODO : Represent a bunch of other changes

		res
	}

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (db <- config.fieldOpt("damageBonus")) {
			damageModifiers :+= DamageModifier(DamagePredicate.Primary, DamageDelta.DamageBonus(db.int))
		}
		for (db <- config.fieldOpt("damageMultiplier")) {
			damageModifiers :+= DamageModifier(DamagePredicate.Primary, DamageDelta.DamageMultiplier(db.float))
		}
		for (db <- config.fieldOpt("damageMalus")) {
			damageModifiers :+= DamageModifier(DamagePredicate.Primary, DamageDelta.DamageBonus(-db.int))
		}
		for (no <- config.fieldOpt("name")) {
			nameOverride = Some(no.str)
		}
		for (hitTargetEffect <- config.fieldAsList("onHitTargetEffects")) {
			GameEffectConfigLoader.loadFrom(hitTargetEffect) match {
				case Some(effect) => triggeredEffects :+= TriggeredAttackEffect(AttackEffectTrigger.Hit, AffectsSelector.Target, effect)
				case None => Noto.error(s"Invalid effect in onHitTargetEffects: $hitTargetEffect")
			}
		}

		for (hitSelfEffect <- config.fieldAsList("onHitSelfEffects")) {
			GameEffectConfigLoader.loadFrom(hitSelfEffect) match {
				case Some(effect) => triggeredEffects :+= TriggeredAttackEffect(AttackEffectTrigger.Hit, AffectsSelector.Self, effect)
				case None => Noto.error(s"Invalid effect in onHitSelfEffects: $hitSelfEffect")
			}
		}
	}
}

object AttackData {
	def renderTriggeredAttackEffects(triggeredEffects : Seq[TriggeredAttackEffect], settings : RichTextRenderSettings): RichText = {
		var text = RichText.Empty
		for (((trigger, affects),all) <- triggeredEffects.groupBy{ e => (e.trigger, e.affects)}) {
			val verb = affects match {
				case AffectsSelector.Self => "receive"
				case AffectsSelector.Target => "apply"
			}
			text += TextSection(s"On $trigger $verb")
			for (TriggeredAttackEffect(_, _, effect) <- all) {
				text += LineBreakSection(0)
				text ++= effect.toRichText(settings)
			}
		}
		text
	}
}

object AttackModifier {
	def loadFromConfig(config: ConfigValue): AttackModifier = {
		val mod = AttackModifier()
		mod.loadFromConfig(config)
		mod
	}
}

@GenerateCompanion
case class DefenseModifier(var dodgeBonus: Int = 0,
									var armorBonus: Int = 0) extends TNestedData

class SpecialAttack {
	var condition: BaseAttackConditional = AnyAttack
	var attackModifier: AttackModifier = AttackModifier()
}

object SpecialAttack {

	def apply(condition : BaseAttackConditional, attackModifier : AttackModifier) : SpecialAttack = {
		val sa = new SpecialAttack
		sa.condition = condition
		sa.attackModifier = attackModifier
		sa
	}

	case object PiercingStabCondition extends BaseAttackConditional {
		val wrapped = (AttackConditionals.HasTargetPattern(Point) or AttackConditionals.HasTargetPattern(TargetPattern.SingleEnemy)) and AttackConditionals.HasDamageType(Piercing) and AttackConditionals.HasAtLeastMaxRange(2)

		override def isTrueFor(implicit view: WorldView, value: BaseAttackProspect): Boolean = wrapped.isTrueFor(view, value)

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText.parse("piercing reach attack", settings)
	}

	case object PiercingStab extends SpecialAttack {
		condition = PiercingStabCondition
		attackModifier = AttackModifier(
			nameOverride = Some("piercing stab"),
			accuracyBonus = -1,
			targetPatternOverride = Some(TargetPattern.Line(startDist = 1, length = 2)),
			minRangeOverride = Some(1),
			maxRangeOverride = Some(1)
		)
	}

	case object PowerAttack extends SpecialAttack {
		condition = AttackConditionals.AnyAttack
		attackModifier = AttackModifier(
			namePrefix = Some("power attack : "),
			accuracyBonus = -2,
			damageModifiers = Vector(DamageModifier(DamagePredicate.Primary, DamageDelta.DamageMultiplier(2.0f)))
		)
	}

	case object SwiftStrike extends SpecialAttack {
		condition = AttackConditionals.HasDamageType(Piercing)
		attackModifier = AttackModifier(
			nameOverride = Some("swift strike"),
			accuracyBonus = -1,
			damageModifiers = Vector(DamageModifier(DamagePredicate.Primary, DamageDelta.DamageBonus(-1))),
			actionCostDelta = -1,
			actionCostMinimum = Some(1)
		)
	}

	import arx.Prelude._

	private lazy val byNameMap = ReflectionAssistant.instancesOfSubtypesOf[SpecialAttack].map(inst => inst.getClass.getSimpleName.replace("$", "").toLowerCase.stripWhitespace -> inst).toMap

	def withNameExists(name: String) = byNameMap.contains(name.toLowerCase().stripWhitespace)

	def withName(name: String) = byNameMap(name.toLowerCase().stripWhitespace)
}


object DamageType extends TEagerSingleton {
	val Physical = Taxonomy("DamageType")

	val Bludgeoning = Taxonomy("DamageTypes.Bludgeoning")

	val Piercing = Taxonomy("DamageTypes.Piercing")

	val Slashing = Taxonomy("DamageTypes.Slashing")

	val Unknown = Taxonomy("DamageTypes.Unknown")
}

case class DamageElement(key: DamageKey, damageDice: DicePool, damageBonus: Int, damageMultiplier: Float, damageType: Taxon) extends THasRichTextRepresentation with ConfigLoadable {
	override def toRichText(settings: RichTextRenderSettings): RichText = {
		import arx.Prelude._
		var sections = Seq[RichTextSection]()
		if (damageDice.nonEmpty) {
			sections :+= TextSection(damageDice.toString())
		}
		if (damageBonus != 0) {
			val str = if (sections.nonEmpty) {
				damageBonus.toSignedString
			} else {
				damageBonus.toString
			}
			sections :+= TextSection(str)
		}
		if (damageMultiplier != 1.0f) {
			sections :+= TextSection("x" + damageMultiplier)
		}

		sections ++= TaxonSections(damageType, settings)
		RichText(sections)
	}
}

case class DamageModifier(predicate: DamagePredicate, delta: DamageDelta)
object DamageModifier extends CustomConfigDataLoader[DamageModifier] {
	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[DamageModifier]

	override def loadFrom(config: ConfigValue): Option[DamageModifier] = {
		(DamagePredicate.loadFrom(config.predicate), DamageDelta.loadFrom(config.delta)) match {
			case (Some(pred), Some(mod)) => Some(DamageModifier(pred, mod))
			case _ => None
		}
	}
}

trait DamagePredicate {
	def matches(de: DamageElement): Boolean
}

object DamagePredicate extends CustomConfigDataLoader[DamagePredicate] {

	case object Primary extends DamagePredicate {
		override def matches(de: DamageElement): Boolean = de.key == DamageKey.Primary
	}

	case object All extends DamagePredicate {
		override def matches(de: DamageElement): Boolean = true
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[DamagePredicate]

	override def loadFrom(config: ConfigValue): Option[DamagePredicate] = {
		config.str.toLowerCase match {
			case "primary" => Some(DamagePredicate.Primary)
			case "all" => Some(DamagePredicate.All)
			case other =>
				Noto.warn(s"Unknown damage predicate : $other, defaulting to Primary")
				None
		}
	}
}

trait DamageDelta extends THasRichTextRepresentation {
	def modify(elem: DamageElement): DamageElement
}

object DamageDelta extends CustomConfigDataLoader[DamageDelta] {

	import arx.Prelude._

	case class DamageBonus(amount: Int) extends DamageDelta {
		override def modify(elem: DamageElement): DamageElement = elem.copy(damageBonus = elem.damageBonus + amount)

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(amount.toSignedString) :: TaxonSections("Damage", settings))
	}

	case class DamageMultiplier(mult: Float) extends DamageDelta {
		override def modify(elem: DamageElement): DamageElement = elem.copy(damageMultiplier = elem.damageMultiplier * mult)

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText(TextSection(s"x$mult") :: TaxonSections("Damage", settings))
	}

	case class SetDamageType(damageType: Taxon) extends DamageDelta {
		override def modify(elem: DamageElement): DamageElement = elem.copy(damageType = damageType)

		override def toRichText(settings: RichTextRenderSettings): RichText = RichText.parse(s"Does [$damageType] damage", settings)
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[DamageDelta]

	val bonusPattern = "([+-]\\d+)".r
	val multPattern = "([x*]\\d+)".r
	val damageTypePattern = "(?i)damage\\s?type\\s?\\(([a-zA-Z]+)\\)".r
	override def loadFrom(config: ConfigValue): Option[DamageDelta] = {
		if (config.isStr) {
			config.str match {
				case bonusPattern(number) => Some(DamageBonus(number.toInt))
				case multPattern(multBy) => Some(DamageMultiplier(multBy.toInt))
				case damageTypePattern(dt) => Some(SetDamageType(Taxonomy(dt, "DamageTypes")))
				case _ => None
			}
		} else {
			None
		}
	}
}


//case class DamageElementDelta(damageDice: Option[DicePool] = None, damageBonus: Option[Int] = None, damageMultiplier: Option[Float] = None, damageType: Option[Taxon] = None) extends THasRichTextRepresentation {
//	override def toRichText(settings: RichTextRenderSettings): RichText = {
//		import arx.Prelude._
//		var sections = Seq[RichTextSection]()
//		for (dd <- damageDice) {
//			sections :+= TextSection(dd.toString())
//		}
//		for (db <- damageBonus) {
//			sections :+= TextSection(db.toSignedString)
//		}
//		for (dm <- damageMultiplier) {
//			sections :+= TextSection("x" + dm)
//		}
//		for (dt <- damageType) {
//			sections :+= TextSection(dt.name)
//		}
//		RichText(sections)
//	}
//}

//object DamageElementDelta {
//	def damageBonus(n: Int) = DamageElementDelta(damageBonus = Some(n))
//}

object DamageElement extends CustomConfigDataLoader[DamageElement] {
	val damageRegex = "([0-9]+)\\s*d\\s*([0-9]+)\\s*([+-]\\s*[0-9]+)?(.*)?".r
	val bonusRegex = "([+-])\\s*([0-9]+)".r

	def toString(elements: Traversable[DamageElement]) = {
		elements.map(e => e.damageDice.toString)
	}


	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[DamageElement]

	override def loadFrom(config: ConfigValue): Option[DamageElement] = {
		if (config.isStr) {
			parse(config.str) match {
				case de@Some(_) => de
				case None =>
					Noto.warn(s"Invalid damage element string : ${config.str}, returning 1 damage")
					None
			}
		} else {
			Some(
				DamageElement(
					config.fieldOpt("key").flatMap(DamageKey.loadFrom).getOrElse(Primary),
					config.fieldOpt("dice").flatMap(DicePool.loadFrom).getOrElse(DicePool.none),
					config.fieldOpt("bonus").map(_.int).getOrElse(0),
					config.fieldOpt("multiplier").map(_.float).getOrElse(0.0f),
					config.fieldOpt("damageType").map(c => Taxonomy(c.str, "DamageTypes")).getOrElse(DamageType.Physical)
				)
			)
		}
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

				Some(DamageElement(DamageKey.Primary, DicePool(dieCount).d(pips), bonus.getOrElse(0), 1.0f, damageType))
			case _ => None
		}
	}
}

trait BaseAttackProspect {
	def attacker: Entity

	def attackData: AttackData
}

case class UntargetedAttackProspect(attacker: Entity, attackData: AttackData) extends BaseAttackProspect

case class AttackProspect(attacker: Entity, target: Entity, allTargets: Seq[Entity], attackData: AttackData, defenseData: DefenseData) extends BaseAttackProspect


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

trait HexTargetPattern extends TargetPattern with THasRichTextRepresentation {
	def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3]
}

object TargetPattern extends CustomConfigDataLoader[TargetPattern] {

	val richTextHexColor = RGBA(0.8f, 0.4f, 0.4f, 1.0f)

	case class Enemies(count: Int) extends EntityTarget {
		override def matches(view: WorldView, attacker: Entity, target: Entity): Boolean = AllegianceLogic.areEnemies(attacker, target)(view)
	}

	def SingleEnemy = Enemies(1)

	case object Point extends HexTargetPattern {
		override def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3] = targetPoint :: Nil

		override def toRichText(settings: RichTextRenderSettings): RichText = ImageSection("graphics/ui/vertical_hex.png", RichTextScale.ScaleToText(true, settings.scale), richTextHexColor)
	}

	case class Line(startDist: Int, length: Int) extends HexTargetPattern {
		override def targetedHexes(sourcePoint: AxialVec3, targetPoint: AxialVec3): Seq[AxialVec3] = {
			//			val sourceCart = sourcePoint.qr.asCartesian
			//			val delta = CartVec((targetPoint.qr.asCartesian - sourceCart).normalizeSafe)
			//			(startDist until (startDist + length)).map(i => AxialVec.fromCartesian(sourceCart + delta * i.toFloat)).map(ax => AxialVec3(ax.q, ax.r, sourcePoint.l))
			val q = sourcePoint.sideClosestTo(targetPoint, AxialVec3.Zero)
			(startDist until (startDist + length)).map(i => AxialVec3(sourcePoint.plusDir(q, i), sourcePoint.l))
		}

		override def toRichText(settings: RichTextRenderSettings): RichText = {
			val sections = for (i <- 1 until startDist + length) yield {
				val img = if (i < startDist) {
					"graphics/ui/vertical_hex_dashed_outline.png"
				} else {
					"graphics/ui/vertical_hex.png"
				}
				ImageSection(img, RichTextScale.ScaleToText(true, settings.scale), richTextHexColor)
			}
			RichText(sections)
		}
	}

	override def loadedType: AnyRef = scala.reflect.runtime.universe.typeOf[TargetPattern]

	private val singleEnemyPattern = "(?i)SingleEnemy".r
	private val enemiesPattern = "(?i)Enemies\\(([0-9]+)\\)".r
	private val linePattern = "(?i)Line\\(([0-9]+),([0-9]+)\\)".r
	private val pointPattern = "(?i)Point".r

	override def loadFrom(config: ConfigValue): Option[TargetPattern] = {
		config.str match {
			case singleEnemyPattern() => Some(SingleEnemy)
			case enemiesPattern(enemyCount) => Some(Enemies(enemyCount.toInt))
			case linePattern(start, length) => Some(Line(start.toInt, length.toInt))
			case pointPattern() => Some(Point)
			case _ =>
				Noto.warn(s"Invalid target pattern config : $config")
				None
		}
	}
}