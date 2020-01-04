package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.Companions.{CharacterInfo, CombatData, Equipment, Physical, Weapon}
import arx.ax4.game.entities.{AttackData, AttackModifier, AttackProspect, AttackReference, CharacterInfo, CombatData, DamageResult, DamageType, DefenseData, DefenseModifier, Physical, SpecialAttack, Tiles, UntargetedAttackProspect, Weapon}
import arx.ax4.game.event.{AttackEvent, AttackEventInfo, DamageEvent, DeflectEvent, DodgeEvent, StrikeEvent, SubStrike}
import arx.core.vec.coordinates.{AxialVec3, BiasedAxialVec3}
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.{World, WorldView}
import arx.game.logic.Randomizer
import arx.core.introspection.FieldOperations._

object CombatLogic {



	def attack(world: World, attacker: Entity, targets: Seq[Entity], attackReference: AttackReference): Unit = {
		implicit val view = world.view
		implicit val randomizer = Randomizer(world)

		val weapon = attackReference.weapon
		attackReference.resolve() match {
			case Some(weaponAttackData) =>
				val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
				val (untargetedAttackData, _) = resolveConditionalAttackData(view, attacker, attackReference, Entity.Sentinel, targets, baseAttackData, new DefenseData)

				world.startEvent(AttackEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))
				world.modify(attacker, CharacterInfo.actionPoints reduceBy untargetedAttackData.actionCost)
				world.modify(attacker, CharacterInfo.stamina reduceBy untargetedAttackData.staminaCost)

				for (strikeN <- 0 until untargetedAttackData.strikeCount) {
					world.startEvent(StrikeEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))

					for (target <- targets) {
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

				for (weaponSkill <- weapon(Weapon).weaponSkills) {
					SkillsLogic.gainSkillXP(attacker, weaponSkill, 10)(world)
				}
				world.endEvent(AttackEvent(AttackEventInfo(attacker, weapon, targets, untargetedAttackData)))

			case None => Noto.error("invalid attack reference")
		}
	}

	def effectiveAttackData(attacker : Entity, from : AxialVec3, targets : Seq[Entity], attackReference: AttackReference)(implicit view : WorldView) = {
		val weapon = attackReference.weapon

		var strikesByTarget = Map[Entity,Vector[EffectiveStrikeData]]()

		attackReference.resolve() match {
			case Some(weaponAttackData) =>
				val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
				val (untargetedAttackData, _) = resolveConditionalAttackData(view, attacker, attackReference, Entity.Sentinel, targets, baseAttackData, new DefenseData)

				for (_ <- 0 until untargetedAttackData.strikeCount) {
					for (target <- targets) yield {
						// resolve raw defense, assuming no information about the attack
						val (rawDefenseData, _) = resolveUnconditionalDefenseData(view, target)
						val (effAttack, _) = resolveConditionalAttackData(view, attacker, attackReference, target, targets, baseAttackData, rawDefenseData)
						val (effDefense, _) = resolveConditionalDefenseData(view, attacker, attackReference, target, targets, effAttack, rawDefenseData)

						strikesByTarget += (target -> (strikesByTarget.getOrElse(target, Vector()) :+ EffectiveStrikeData(effAttack, effDefense)))
					}
				}
			case None => Noto.error("invalid attack reference")
		}

		EffectiveAttackData(strikesByTarget)
	}

	def doDamage(world: World, target: Entity, damage: Int, damageType: Taxon, defenseData: DefenseData, source: Option[String]): Unit = {
		val effDamage = if (damageType.isA(DamageType.Physical)) {
			damage - defenseData.armor
		} else {

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

	def resolveUntargetedConditionalAttackData(view: WorldView, attacker: Entity, attackRef: AttackReference): Option[(AttackData, Vector[(String, AttackModifier)])] = {
		val weapon = attackRef.weapon
		attackRef.resolve()(view).map(weaponAttackData => {
			val (baseAttackData, _) = resolveUnconditionalAttackData(view, attacker, weapon, weaponAttackData)
			resolveConditionalAttackData(view, attacker, attackRef, Entity.Sentinel, Nil, baseAttackData, new DefenseData)
		})
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


	def resolveConditionalAttackData(view: WorldView, attacker: Entity, attackReference: AttackReference, target: Entity, allTargets: Seq[Entity], baseAttackData: AttackData, baseDefenseData: DefenseData): (AttackData, Vector[(String, AttackModifier)]) = {
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
			} else {
				Noto.error("unlisted unconditional defense source")
			}
		}

		defenseData -> modifiers
	}

	def resolveConditionalDefenseData(view: WorldView, attacker: Entity, attackReference: AttackReference, target: Entity, allTargets: Seq[Entity], baseAttackData: AttackData, baseDefenseData: DefenseData): (DefenseData, Vector[(String, DefenseModifier)]) = {
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

	def availableAttacks(attacker: Entity, includeBaseAttacks : Boolean, includeSpecialAttacks: Boolean)(implicit view: WorldView): Seq[AttackReference] = {
		val weaponAttacks = attacker(Equipment).equipped
			.filter(e => e.hasData[Weapon])
			.flatMap(weapon => weapon(Weapon).attacks.map { case (k, _) => AttackReference(weapon, k, None) })

		if (includeSpecialAttacks) {
			val specialAttackSources = (attacker(Equipment).equipped + attacker)
				.filter(e => e.hasData[CombatData])
				.flatMap(e => e(CombatData).specialAttacks.map(sa => e -> sa))

			val specialAttacks = specialAttackSources.flatMap { case (src, (sak, sav)) =>
				weaponAttacks.flatMap(war => {
					if (sav.condition.isTrueFor(view, UntargetedAttackProspect(attacker, war))) {
						Some(war.copy(specialAttack = Some(sav)))
					} else {
						None
					}
				})
			}

			if (includeBaseAttacks) {
				(weaponAttacks ++ specialAttacks).toSeq
			} else {
				specialAttacks.toSeq
			}
		} else {
			if (includeBaseAttacks) {
				weaponAttacks.toSeq
			} else {
				Nil
			}
		}
	}

	def validSpecialAttackPermutations(attacker : Entity, specialAttack : SpecialAttack)(implicit view : WorldView) : List[AttackReference] = {
		val rawAttacks = availableAttacks(attacker, includeBaseAttacks = true, includeSpecialAttacks = false)
		rawAttacks.filter(a => specialAttack.condition.isTrueFor(view, UntargetedAttackProspect(attacker, a)))
   		.map(a => AttackReference(a.weapon, a.attackKey, Some(specialAttack)))
   		.toList
	}

	def resolveSpecialAttack(attacker : Entity, specialAttack : SpecialAttack)(implicit view : WorldView) : Option[AttackReference] = {
		validSpecialAttackPermutations(attacker, specialAttack).headOption
	}

	def targetedEntities(targets : Either[Seq[Entity], Seq[BiasedAxialVec3]])(implicit view : WorldView) : Seq[Entity] = {
		targets match {
			case Left(value) => value
			case Right(value) => value.flatMap(h => Tiles.entitiesOnTile(h.vec))
		}
	}

	def baseAttackDataForWeapon(weapon : Entity)(implicit view : WorldView) : AttackData = {
		AttackReference(weapon, weapon[Weapon].primaryAttack, None).resolve() match {
			case Some(attackData) => attackData
			case None =>
				Noto.warn(s"Weapon did not have primary attack $weapon")
				AttackData(name = "Could not resolve")
		}
	}
}


case class EffectiveStrikeData(attackData : AttackData, defenseData : DefenseData)
case class EffectiveAttackData(strikesByTarget : Map[Entity, Vector[EffectiveStrikeData]])