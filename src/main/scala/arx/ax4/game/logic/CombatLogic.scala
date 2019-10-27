package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.entities.{AttackData, AttackModifier, AttackProspect, AttackReference, CharacterInfo, CombatData, DamageResult, DamageType, DefenseData, DefenseModifier, Weapon}
import arx.ax4.game.event.{AttackEventInfo, DamageEvent, DeflectEvent, DodgeEvent, StrikeEvent, SubStrike}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.game.logic.Randomizer

object CombatLogic {
	import arx.core.introspection.FieldOperations._

	def attack(world: World, attacker: Entity, targets: List[Entity], attackReference: AttackReference): Unit = {
		implicit val view = world.view
		implicit val randomizer = Randomizer(world)

		val weapon = attackReference.weapon
		attackReference.resolve() match {
			case Some(weaponAttackData) =>
				val (baseAttackData,_) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
				val (untargetedAttackData,_) = resolveConditionalAttackData(view, attacker, attackReference, Entity.Sentinel, targets, baseAttackData, new DefenseData)

				for (strikeN <- 0 until untargetedAttackData.strikeCount) {
					world.startEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))

					for (target <- targets) yield {
						// resolve raw defense, assuming no information about the attack
						val (rawDefenseData, _) = resolveUnconditionalDefenseData(view, target)
						val (effAttack, _) = resolveConditionalAttackData(view, attacker, attackReference, target, targets, baseAttackData, rawDefenseData)
						val (effDefense, _) = resolveConditionalDefenseData(view, attacker, attackReference, target, targets, effAttack, rawDefenseData)
						world.startEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))

						val accuracyRoll = randomizer.stdRoll().total - 10
						val toHitScore = accuracyRoll + effAttack.accuracyBonus - effDefense.dodgeBonus

						if (toHitScore > 0) {
							for ((src, dmg) <- effAttack.damage) {
								doDamage(world, target, ((dmg.damageDice.roll().total + dmg.damageBonus) * dmg.damageMultiplier).toInt, dmg.damageType, effDefense, Some(src.toString))
							}
						} else {
							world.addEvent(DodgeEvent(target))
						}

						world.endEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))
					}

					world.endEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
				}


			case None => Noto.error("invalid attack reference")
		}
	}

	def doDamage(world : World, target : Entity, damage : Int, damageType : DamageType, defenseData : DefenseData, source : Option[String]): Unit = {
		val effDamage = if (damageType.isA(DamageType.Physical)) {
			damage - defenseData.armor
		} else {
			// TODO: elemental resistances or whatnot
			damage
		}.min(world.view.data[CharacterInfo](target).health.currentValue)

		if (effDamage > 0) {
			world.startEvent(DamageEvent(target, effDamage, damageType))
			world.modify(target, CharacterInfo.health reduceBy effDamage, source)
			world.endEvent(DamageEvent(target, effDamage, damageType))
		} else {
			world.addEvent(DeflectEvent(target, damage))
		}
	}


	def resolveUnconditionalAttackData(view: WorldView, attacker: Entity, weapon: Entity, baseAttackData: AttackData): (AttackData, Vector[(String, AttackModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, AttackModifier)]()

		val attackData = baseAttackData.copy()
		for (modifyingEntity <- List(attacker, weapon).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			attackData.merge(combatData.attackModifier)
			if (modifyingEntity == attacker) {
				modifiers :+= "character" -> combatData.attackModifier
			} else if(modifyingEntity == weapon) {
				modifiers :+= "weapon" -> combatData.attackModifier
			} else { Noto.error("unlisted unconditional attack source") }
		}

		attackData -> modifiers
	}

	def resolveConditionalAttackData(view: WorldView, attacker: Entity, attackReference: AttackReference, target: Entity, allTargets : List[Entity], baseAttackData: AttackData, baseDefenseData: DefenseData): (AttackData, Vector[(String, AttackModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, AttackModifier)]()

		val attackData = baseAttackData.copy()
		val prospect = AttackProspect(attacker, attackReference, target, allTargets, attackData, baseDefenseData)

		for (modifyingEntity <- List(attacker, attackReference.weapon).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			for ((cond, mod) <- combatData.conditionalAttackModifiers; if cond.isTrueFor(view, prospect)) {
				attackData.merge(mod)
				modifiers :+= cond.source -> mod
			}
		}

		attackData -> modifiers
	}

	def resolveUnconditionalDefenseData(view: WorldView, target: Entity): (DefenseData, Vector[(String, DefenseModifier)]) = {
		implicit val implView = view
		var modifiers = Vector[(String, DefenseModifier)]()

		val defenseData = new DefenseData()
		for (modifyingEntity <- List(target).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			defenseData.merge(combatData.defenseModifier)
			if (modifyingEntity == target) {
				modifiers :+= "character" -> combatData.defenseModifier
			} else { Noto.error("unlisted unconditional defense source") }
		}

		defenseData -> modifiers
	}

	def resolveConditionalDefenseData(view: WorldView, attacker: Entity, attackReference: AttackReference, target: Entity, allTargets : List[Entity], baseAttackData: AttackData, baseDefenseData : DefenseData): (DefenseData, Vector[(String, DefenseModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, DefenseModifier)]()

		val defenseData = baseDefenseData.copy()
		val prospect = AttackProspect(attacker, attackReference, target, allTargets, baseAttackData, defenseData)

		for (modifyingEntity <- List(target).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			for ((cond, mod) <- combatData.conditionalDefenseModifiers; if cond.isTrueFor(view, prospect)) {
				defenseData.merge(mod)
				modifiers :+= cond.source -> mod
			}
		}

		defenseData -> modifiers
	}

}