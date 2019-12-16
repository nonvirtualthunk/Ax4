package arx.ax4.game.entities

import arx.ax4.game.entities.Companions.CombatData
import arx.engine.data.TAuxData
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


case class Skill(displayName : String, var levels: List[SkillLevel]) {
	def skillLevel(n : Int) = levels.lift(n - 1)
}

object SkillsLibrary {

	import arx.core.introspection.FieldOperations._
	import AttackConditionals._

	var byTaxon: Map[Taxon, Skill] = Map()

	def apply(skill : Taxon) : Option[Skill] = byTaxon.get(skill)

	byTaxon += Taxonomy("spearSkill", "Skills") -> Skill("Spear",
		List(
			SkillLevel(
				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(accuracyBonus = 1))),
				(world, entity) => world.modify(entity, CombatData.specialAttacks.put("piercing stab", SpecialAttack.PiercingStab))
			), SkillLevel(
				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(damageBonuses = Map(AttackKey.Primary -> DamageElementDelta.damageBonus(2)))))
			), SkillLevel(
				(world, entity) => world.modify(entity, CombatData.conditionalAttackModifiers append (AttackConditionals.WeaponIs(Taxonomy("spear", "Items.Weapons")) -> AttackModifier(minRangeOverride = Some(1))))
			)
		)
	)
	byTaxon += Taxonomy("swordSkill", "Skills") -> Skill("Sword",
		List(
			SkillLevel()
				.withModifier(CombatData.conditionalAttackModifiers append (WeaponIs(Taxonomy("sword", "Items.Weapons")) -> AttackModifier(accuracyBonus = 1)))
//   			.withModifier(CombatData.specialAttacks.put(""))
		)
	)
}
