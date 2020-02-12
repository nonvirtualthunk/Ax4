package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.{CharacterInfo, PendingPerkPicks, PerkSource, SkillsLibrary}
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.{GainSkillLevelEvent, GainSkillXPEvent, NewPerkPicksAvailable}
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.{NestedKeyedModifier, NestedModifier, World, WorldView}

object SkillsLogic {
	import arx.core.introspection.FieldOperations._

	def xpForLevel(level : Int) = {
		if (level <= 0) {
			0
		} else {
			10 + (level * (level - 1)) * 10
		}

		// 10 + 0, 2, 6, 12, 20, 30,
		// 10 + 0, 20, 60, 120, 200, 300
		// 10, 30, 70, 130, 210, 310
	}

	def currentLevelXp(character : Entity, skill : Taxon)(implicit view : WorldView) = {
		val curLevel = skillLevel(character, skill)
		character(CharacterInfo).skillXP.getOrElse(skill, 0) - xpForLevel((curLevel).max(0))
	}

	def currentLevelXpRequired(character : Entity, skill : Taxon)(implicit view : WorldView) = {
		val curL = skillLevel(character, skill)
		xpForLevel(curL+1) - xpForLevel(curL)
	}

	def skillLevels(character : Entity)(implicit view :WorldView) = {
		character(CharacterInfo).skillLevels
	}

	def skillLevel(character : Entity, skill : Taxon)(implicit view : WorldView) = {
		character(CharacterInfo).skillLevels.getOrElse(skill, 0)
	}

	/**
	 * Determines the effective skill level when multiple skills are applied in a single context. The highest
	 * skill counts for full, the second for half, the third for one third, etc
	 */
	def effectiveSkillLevel(character : Entity, skills : Seq[Taxon])(implicit view : WorldView) = {
		val levelsBySkill = skills.map(skill => skill -> SkillsLogic.skillLevel(character, skill)).sortBy(-_._2)
		var effectiveSkillLevel = 0
		var levelDivisor = 1

		for ((skill, level) <- levelsBySkill) {
			effectiveSkillLevel += level / levelDivisor
			levelDivisor += 1
		}

		effectiveSkillLevel
	}

	def xpGainFor(effectiveSkillLevel : Int, difficulty : Int) = 5 - (effectiveSkillLevel - difficulty)

	def gainSkillXP(character : Entity, skills : Seq[Taxon], amount : Int)(implicit world : World): Unit = {
		for (skill <- skills) {
			gainSkillXP(character, skill, amount / skills.size)
		}
	}

	def gainSkillXP(character : Entity, skill : Taxon, amount : Int)(implicit world : World): Unit = {
		implicit val view = world.view

		val charInfo = character(CharacterInfo)
		val curXP = charInfo.skillXP.getOrElse(skill, 0)
		val newXP = curXP + amount

		val curLevel = skillLevel(character, skill)
		var newLevel = curLevel
		while (newXP >= xpForLevel(newLevel+1)) {
			newLevel += 1
		}

		world.startEvent(GainSkillXPEvent(character, skill, amount))
		world.modify(character, CharacterInfo.skillXP.put(skill, newXP))
		world.endEvent(GainSkillXPEvent(character, skill, amount))

		var markLevel = curLevel + 1
		while (markLevel <= newLevel) {
			world.startEvent(GainSkillLevelEvent(character, skill, markLevel))
			world.modify(character, CharacterInfo.skillLevels.put(skill, markLevel))


			SkillsLibrary.getWithKind(skill) match {
				case Some(skillInfo) =>
					val possiblePerks = skillInfo.levelUpPerks.filter(perk => {
						markLevel >= perk.minLevel && markLevel <= perk.maxLevel &&
							perk.requirements.forall(req => req.isTrueFor(view, character))
					})
					val newPicks = possiblePerks.map(_.perk)
					world.startEvent(NewPerkPicksAvailable(character, newPicks))
					world.modify(character, CharacterInfo.pendingPerkPicks append PendingPerkPicks(newPicks, PerkSource.SkillLevelUp(skill, markLevel)))
					world.endEvent(NewPerkPicksAvailable(character, newPicks))
				case None =>
					Noto.warn(s"No information on skill $skill")
			}

			world.endEvent(GainSkillLevelEvent(character, skill, markLevel))
			markLevel += 1
		}
	}
}
