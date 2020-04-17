package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.action.SelectionResult
import arx.ax4.game.entities.AttackEffectTrigger.Hit
import arx.ax4.game.entities.Companions.{CharacterInfo, CombatData, Equipment, Physical, Weapon}
import arx.ax4.game.entities.{AffectsSelector, AttackData, AttackEffectTrigger, AttackModifier, AttackProspect, CharacterInfo, CombatData, DamageResult, DamageType, DefenseData, DefenseModifier, Physical, SpecialAttack, Tiles, TriggeredAttackEffect, UntargetedAttackProspect, Weapon}
import arx.ax4.game.event.{ArmorUsedEvent, AttackEvent, AttackEventInfo, DamageEvent, DeflectEvent, DodgeEvent, StrikeEvent, SubStrike}
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

		for (strikeN <- 0 until untargetedAttackData.strikeCount) {
			world.startEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))

			for (target <- targets) {
				// resolve raw defense, assuming no information about the attack
				val (rawDefenseData, _) = resolveUnconditionalDefenseData(view, target)
				val (effAttack, _) = resolveConditionalAttackData(view, attacker, target, targets, baseAttackData, rawDefenseData)
				val (effDefense, _) = resolveConditionalDefenseData(view, attacker, target, targets, effAttack, rawDefenseData)
				world.startEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))

				val defenseDeltaFlags = TagLogic.flagValue(target, "DefenseDelta")

				val accuracyRoll = randomizer.stdRoll().total - 10
				val toHitScore = accuracyRoll + effAttack.accuracyBonus - effDefense.dodgeBonus - defenseDeltaFlags

				if (toHitScore > 0) {
					for (dmg <- effAttack.damage) {
						doDamage(world, target, ((dmg.damageDice.roll().total + dmg.damageBonus) * dmg.damageMultiplier).toInt, dmg.damageType, effDefense)
					}
					applyTriggeredEffects(attacker, target, effAttack.triggeredEffects, AttackEffectTrigger.Hit)(world)
				} else {
					world.addEvent(DodgeEvent(target))
					applyTriggeredEffects(attacker, target, effAttack.triggeredEffects, AttackEffectTrigger.Miss)(world)
				}

				world.endEvent(SubStrike(target, AttackEventInfo(attacker, weapon, targets, effAttack), effDefense))
			}

			world.endEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
		}

		for (weaponSkill <- weapon.dataOpt[Weapon].map(_.weaponSkills)) {
			SkillsLogic.gainSkillXP(attacker, weaponSkill, 10)(world)
		}
		world.endEvent(AttackEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
	}

	private def applyTriggeredEffects(attacker : Entity, target : Entity, triggeredEffects : Seq[TriggeredAttackEffect], targetTrigger : AttackEffectTrigger)(implicit world : World): Unit = {
		implicit val view = world.view

		for (TriggeredAttackEffect(trigger, affects, effect) <- triggeredEffects if trigger == targetTrigger) {
			val affectedEntity = affects match {
				case AffectsSelector.Self => attacker
				case AffectsSelector.Target => target
			}
			effect.instantiate(view, affectedEntity, attacker) match {
				case Left(inst) => {
					val sel = SelectionResult()
					inst.nextSelector(sel) match {
						case Some(value) => Noto.warn(s"Attack triggered effect not applied, requires selection implementation : $trigger, $affects, $effect")
						case None => inst.applyEffect(world, sel)
					}
				}
				case Right(msg) => Noto.info(s"Attack triggered effect not applied: $msg")
			}
		}
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
			val armorDeltaFlags = TagLogic.flagValue(target, "ArmorDelta")(world.view)
			val totalArmor = defenseData.armor - armorDeltaFlags
			world.addEvent(ArmorUsedEvent(target, damage, totalArmor, damageType))
			damage - totalArmor
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

	def canAttackBeMade(attacker: Entity, attackerPos: AxialVec3, target: Either[Entity, AxialVec3], attackData: AttackData)(implicit worldView: WorldView): Boolean = {
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

			for ((flag, flagModifiers) <- TagLogic.allFlagAttackModifiers(attacker); modifier <- flagModifiers) {
				attackData.merge(modifier)
				modifiers :+= flag.displayName -> modifier
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
				modifiers :+= cond.toString -> mod
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
				modifiers :+= cond.toString -> mod
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
   		.flatMap(weapon => weapon[Weapon].attacks.toVector.sortBy(_._1).map(weapon -> _._2))
   		.toVector
	}

	def availableAttacks(attacker : Entity)(implicit view : WorldView) : Vector[(Entity, AttackData)] = {
		val weaponAttacks = availableWeaponAttacks(attacker)
		if (weaponAttacks.isEmpty) {
			attacker.dataOpt[Weapon] match {
				case Some(weapon) => weapon.attacks.values.map(attacker -> _).toVector
				case None => Vector()
			}
		} else {
			weaponAttacks
		}
	}

	def resolveSpecialAttack(attacker: Entity, specialAttack: SpecialAttack)(implicit view : WorldView) : Option[AttackData] = {
		val allAttacks = CombatLogic.availableAttacks(attacker)
		allAttacks.find(attack => specialAttack.condition.isTrueFor(view, UntargetedAttackProspect(attacker, attack._2)))
  		.map { case ((weapon, attack)) =>
				val effAttack = attack.mergedWith(specialAttack.attackModifier)
				effAttack.weapon = weapon
				effAttack
			}
	}
}


case class EffectiveStrikeData(attackData : AttackData, defenseData : DefenseData)
case class EffectiveAttackData(strikesByTarget : Map[Entity, Vector[EffectiveStrikeData]])