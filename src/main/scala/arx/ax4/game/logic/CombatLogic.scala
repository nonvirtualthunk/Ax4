package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, CombatData, Equipment, Physical, Weapon}
import arx.ax4.game.entities.{AttackData, AttackModifier, AttackProspect, CharacterInfo, CombatData, DamageResult, DamageType, DefenseData, DefenseModifier, Physical, SpecialAttack, Tiles, UntargetedAttackProspect, Weapon}
import arx.ax4.game.event.{AttackEvent, AttackEventInfo, DamageEvent, DeflectEvent, DodgeEvent, StrikeEvent, SubStrike}
import arx.core.vec.coordinates.{AxialVec3, BiasedAxialVec3}
import arx.engine.entity.{Entity, Taxon, Taxonomy}
import arx.engine.world.{World, WorldView}
import arx.game.logic.Randomizer
import arx.core.introspection.FieldOperations._

object CombatLogic {



	def attack(world: World, attacker: Entity, targets: Seq[Entity], weaponAttackData: AttackData): Unit = {
		implicit val view = world.view
		implicit val randomizer = Randomizer(world)

		val weapon = weaponAttackData.weapon

		val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
		val (untargetedAttackData, _) = resolveConditionalAttackData(view, attacker, Entity.Sentinel, targets, baseAttackData, new DefenseData)

		world.startEvent(AttackEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
//		world.modify(attacker, CharacterInfo.actionPoints reduceBy untargetedAttackData.actionCost)
//		world.modify(attacker, CharacterInfo.stamina reduceBy untargetedAttackData.staminaCost)

		for (strikeN <- 0 until untargetedAttackData.strikeCount) {
			world.startEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))

			for (target <- targets) {
				// resolve raw defense, assuming no information about the attack
				val (rawDefenseData, _) = resolveUnconditionalDefenseData(view, target)
				val (effAttack, _) = resolveConditionalAttackData(view, attacker, target, targets, baseAttackData, rawDefenseData)
				val (effDefense, _) = resolveConditionalDefenseData(view, attacker, target, targets, effAttack, rawDefenseData)
				world.startEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))

				val parry = TagLogic.flagValue(target, "parry")

				val accuracyRoll = randomizer.stdRoll().total - 10
				val toHitScore = accuracyRoll + effAttack.accuracyBonus - effDefense.dodgeBonus - parry

				if (toHitScore > 0) {
					for (dmg <- effAttack.damage) {
						doDamage(world, target, ((dmg.damageDice.roll().total + dmg.damageBonus) * dmg.damageMultiplier).toInt, dmg.damageType, effDefense)
					}
				} else {
					world.addEvent(DodgeEvent(target))
				}

				world.endEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))
			}

			world.endEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
		}

		for (weaponSkill <- weapon(Weapon).weaponSkills) {
			SkillsLogic.gainSkillXP(attacker, weaponSkill, 10)(world)
		}
		world.endEvent(AttackEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
	}

	def effectiveAttackData(attacker : Entity, from : AxialVec3, targets : Seq[Entity], weaponAttackData: AttackData)(implicit view : WorldView) = {
		val weapon = weaponAttackData.weapon

		var strikesByTarget = Map[Entity,Vector[EffectiveStrikeData]]()


		val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
		val (untargetedAttackData, _) = resolveConditionalAttackData(view, attacker, Entity.Sentinel, targets, baseAttackData, new DefenseData)

		for (_ <- 0 until untargetedAttackData.strikeCount) {
			for (target <- targets) yield {
				// resolve raw defense, assuming no information about the attack
				val (rawDefenseData, _) = resolveUnconditionalDefenseData(view, target)
				val (effAttack, _) = resolveConditionalAttackData(view, attacker, target, targets, baseAttackData, rawDefenseData)
				val (effDefense, _) = resolveConditionalDefenseData(view, attacker, target, targets, effAttack, rawDefenseData)

				strikesByTarget += (target -> (strikesByTarget.getOrElse(target, Vector()) :+ EffectiveStrikeData(effAttack, effDefense)))
			}
		}

		EffectiveAttackData(strikesByTarget)
	}

	def doDamage(world: World, target: Entity, damage: Int, damageType: Taxon, defenseData: DefenseData): Unit = {
		val effDamage = if (damageType.isA(DamageType.Physical)) {
			damage - defenseData.armor
		} else {

			damage
			}.min(world.view.data[CharacterInfo](target).health.currentValue)

		if (effDamage > 0) {
			world.startEvent(DamageEvent(target, effDamage, damageType))
			world.modify(target, CharacterInfo.health reduceBy effDamage)
			world.endEvent(DamageEvent(target, effDamage, damageType))
		} else {
			world.addEvent(DeflectEvent(target, damage, damageType))
		}
	}

	def canAttackBeMade(implicit worldView: WorldView, attacker: Entity, attackerPos: AxialVec3, target: Either[Entity, AxialVec3], attackData: AttackData): Boolean = {
		if (attackData.actionCost > attacker(CharacterInfo).actionPoints.currentValue) {
			false
		} else if (attackData.staminaCost * attackData.strikeCount > attacker(CharacterInfo).stamina.currentValue) {
			false
		} else {
			target match {
				case Left(targetEnt) =>
					worldView.dataOpt[Physical](targetEnt) match {
						case Some(p) =>
							val dist = attackerPos.distance(p.position).toInt
							if (dist >= attackData.minRange && dist <= attackData.maxRange) {
								true
							} else {
								false
							}
						case None => false
					}
				case Right(hex) =>
					val dist = attackerPos.distance(hex).toInt
					if (dist >= attackData.minRange && dist <= attackData.maxRange) {
						true
					} else {
						false
					}
			}
		}
	}

	def resolveUntargetedConditionalAttackData(view: WorldView, attacker: Entity, weaponAttackData : AttackData): (AttackData, Vector[(String, AttackModifier)]) = {
		val weapon = weaponAttackData.weapon

		val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
		resolveConditionalAttackData(view, attacker, Entity.Sentinel, Nil, baseAttackData, new DefenseData)
	}

	def resolveUnconditionalAttackData(view: WorldView, attacker: Entity, weapon: Entity, baseAttackData: AttackData): (AttackData, Vector[(String, AttackModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, AttackModifier)]()

		val attackData = baseAttackData.copy()
		for (modifyingEntity <- List(attacker, weapon).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			attackData.merge(combatData.attackModifier)
			if (modifyingEntity == attacker) {
				modifiers :+= "character" -> combatData.attackModifier
			} else if (modifyingEntity == weapon) {
				modifiers :+= "weapon" -> combatData.attackModifier
			} else {
				Noto.error("unlisted unconditional attack source")
			}
		}


		attackData -> modifiers
	}


	def resolveConditionalAttackData(view: WorldView, attacker: Entity, target: Entity, allTargets: Seq[Entity], baseAttackData: AttackData, baseDefenseData: DefenseData): (AttackData, Vector[(String, AttackModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, AttackModifier)]()

		val attackData = baseAttackData.copy()
		val prospect = AttackProspect(attacker, target, allTargets, attackData, baseDefenseData)

		for (modifyingEntity <- List(attacker, attackData.weapon).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
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

		val defenseData = DefenseData()
		for (modifyingEntity <- List(target).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			defenseData.merge(combatData.defenseModifier)
			if (modifyingEntity == target) {
				modifiers :+= "character" -> combatData.defenseModifier
			} else {
				Noto.error("unlisted unconditional defense source")
			}
		}

		defenseData -> modifiers
	}

	def resolveConditionalDefenseData(view: WorldView, attacker: Entity, target: Entity, allTargets: Seq[Entity], baseAttackData: AttackData, baseDefenseData: DefenseData): (DefenseData, Vector[(String, DefenseModifier)]) = {
		implicit val implView = view

		var modifiers = Vector[(String, DefenseModifier)]()

		val defenseData = baseDefenseData.copy()
		val prospect = AttackProspect(attacker, target, allTargets, baseAttackData, defenseData)

		for (modifyingEntity <- List(target).distinct; combatData <- modifyingEntity.dataOpt[CombatData]) {
			for ((cond, mod) <- combatData.conditionalDefenseModifiers; if cond.isTrueFor(view, prospect)) {
				defenseData.merge(mod)
				modifiers :+= cond.source -> mod
			}
		}

		defenseData -> modifiers
	}

	def targetedEntities(targets : Either[Seq[Entity], Seq[AxialVec3]])(implicit view : WorldView) : Seq[Entity] = {
		targets match {
			case Left(value) => value
			case Right(value) => value.flatMap(h => Tiles.entitiesOnTile(h))
		}
	}

	def availableWeaponAttacks(attacker : Entity)(implicit view : WorldView) : Vector[(Entity, AttackData)] = {
		InventoryLogic.equippedItems(attacker)
			.filter(item => item.hasData[Weapon])
   		.flatMap(weapon => weapon[Weapon].attacks.values.map(weapon -> _))
   		.toVector
	}
}


case class EffectiveStrikeData(attackData : AttackData, defenseData : DefenseData)
case class EffectiveAttackData(strikesByTarget : Map[Entity, Vector[EffectiveStrikeData]])