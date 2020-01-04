package arx.ax4.game.entities

import arx.application.Noto
import arx.ax4.game.entities.Companions.CombatData
import arx.ax4.game.entities.cardeffects.GameEffect
import arx.core.NoAutoLoad
import arx.core.representation.ConfigValue
import arx.engine.data.{ConfigLoadable, TAuxData}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{Modifier, World}

import scala.reflect.ClassTag


case class SkillLevel(onLevelGainedFunctions: List[(World, Entity) => Unit]) {
	def onLevelGained(world: World, entity: Entity) = {
		onLevelGainedFunctions.foreach(f => f(world, entity))
	}

	def withModifier[C <: TAuxData](mod : Modifier[C])(implicit classTag: ClassTag[C]) = {
		val newFunc : (World,Entity) => Unit = (world : World, entity : Entity) => {world.modify(entity, mod)(classTag)}
		copy(onLevelGainedFunctions = newFunc :: onLevelGainedFunctions)
	}
}

object SkillLevel{
	def apply(onLevelGainedFunctions: ((World, Entity) => Unit) *) = new SkillLevel(onLevelGainedFunctions.toList)
}

case class Perk(identity : Taxon, name : String, description : String, effects : Seq[GameEffect])
object Perk {
	val Sentinel = Perk(Taxonomy("UnknownPerk"), "Sentinel", "sentinel", Nil)
}

class SkillLevelUpPerk extends ConfigLoadable {
	var perk : Perk = Perk.Sentinel
	var minLevel : Int = 0
	var maxLevel : Int = 100
	@NoAutoLoad
	var requirements : List[Conditional[Entity]] = Nil

	override def customLoadFromConfig(config: ConfigValue): Unit = {
		for (levelRange <- config.fieldOpt("levelRange")) {
			levelRange.str match {
				case SkillLevelUpPerk.levelRangePattern(lower, upper) =>
					minLevel = lower.toInt
					maxLevel = upper.toInt
				case s => Noto.warn(s"Invalid level range : $s")
			}
		}

		requirements = config.fieldAsList("requires").map(c => EntityConditionals.fromConfig(c))

	}
}
object SkillLevelUpPerk {
	val levelRangePattern = "([0-9]+)-([0-9]+)".r

	def apply(perk : Perk, minLevel : Int, maxLevel : Int, requirements : List[Conditional[Entity]]) : SkillLevelUpPerk = {
		val skill = new SkillLevelUpPerk
		skill.perk = perk
		skill.minLevel = minLevel
		skill.maxLevel = maxLevel
		skill.requirements = requirements
		skill
	}
}

case class  Skill(displayName : String, var levelUpPerks: List[SkillLevelUpPerk]) {

}



object SkillsLibrary {
	// TODO : Bring this in from skills.sml

	import arx.core.introspection.FieldOperations._
	import AttackConditionals._

	var byTaxon: Map[Taxon, Skill] = Map()

	def apply(skill : Taxon) : Option[Skill] = byTaxon.get(skill)

//	byTaxon += Taxonomy("spearSkill", "Skills") -> Skill("Spear",
//		SkillLevelUpPerk(Perk(Taxonomy("SpearProficiency", "Perks"), "Spear Proficiency", ""))
//	)
//
//	byTaxon += Taxonomy("spearSkill", "Skills") -> Skill("Spear",
//		List(
//			SkillLevel(
//				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(accuracyBonus = 1))),
//				(world, entity) => world.modify(entity, CombatData.specialAttacks.put("piercing stab", SpecialAttack.PiercingStab))
//			), SkillLevel(
//				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(damageBonuses = Map(AttackKey.Primary -> DamageElementDelta.damageBonus(2)))))
//			), SkillLevel(
//				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(minRangeOverride = Some(1))))
//			)
//		)
//	)
//	byTaxon += Taxonomy("swordSkill", "Skills") -> Skill("Sword",
//		List(
//			SkillLevel()
//				.withModifier(CombatData.conditionalAttackModifiers append (WeaponIs(Taxonomy("sword", "Items.Weapons")) -> AttackModifier(accuracyBonus = 1)))
////   			.withModifier(CombatData.specialAttacks.put(""))
//		)
//	)
//	byTaxon += Taxonomy("unarmedSkill", "Skills") -> Skill("Unarmed",
//		List(
//			SkillLevel()
//		)
//	)
}
