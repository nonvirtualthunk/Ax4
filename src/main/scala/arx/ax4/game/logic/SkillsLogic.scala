package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.{CardData, CardLibrary, CharacterInfo, LevelUpPerk, PendingPerkPicks, Perk, PerkSource, SkillsLibrary}
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.entities.cardeffects.AddCardToDeck
import arx.ax4.game.event.{GainSkillLevelEvent, GainSkillXPEvent, NewPerkPicksAvailable}
import arx.ax4.game.logic.CardAdditionStyle.DrawPile
import arx.engine.entity.Companions.IdentityData
import arx.engine.entity.{Entity, IdentityData, Taxon, Taxonomy}
import arx.engine.world.{NestedKeyedModifier, NestedModifier, World, WorldView}
import arx.game.logic.Randomizer

object SkillsLogic {
	import arx.core.introspection.FieldOperations._

	def xpForLevel(level : Int) = {
		if (level <= 0) {
			0
		} else {
			10 + (level * (level - 1)) * 10
		}

		// 10, 20, 40, 60, 80, 100 : xp gain required to reach level
		// 10, 30, 70, 130, 210, 310 : total xp by level
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

	def skillXP(character : Entity)(implicit view :WorldView) = {
		character(CharacterInfo).skillXP
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

	def possiblePerksForSkill(character : Entity, skill : Taxon, markLevel : Int)(implicit view : WorldView): Vector[Perk] = {
		val perks = SkillsLibrary.getWithKind(skill) match {
			case Some(skillInfo) =>
				skillInfo.cardRewards.filter(cw => {
					cw.targetLevel <= markLevel
				}).flatMap(cw => {
					CardLibrary.getWithKind(cw.card).map(cardInfo => {
						Perk(
							cw.card,
							"Add Card",
							"",
							Seq(AddCardToDeck(Seq(cw.card), DrawPile)),
							cw.rarity,
							Some("graphics/icons/card_back_large.png")
						)
					})
				})
			case None => Vector()
		}

		perks ++ skill.parents.flatMap(p => possiblePerksForSkill(character, p, markLevel))
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

			val possiblePerks = possiblePerksForSkill(character, skill, markLevel).distinct

			val randomizer = Randomizer(world)
			val newPicks = randomizer.takeRandom(possiblePerks, 3).toVector

			world.startEvent(NewPerkPicksAvailable(character, newPicks))
			world.modify(character, CharacterInfo.pendingPerkPicks append PendingPerkPicks(newPicks, PerkSource.SkillLevelUp(skill, markLevel)))
			world.endEvent(NewPerkPicksAvailable(character, newPicks))

			world.endEvent(GainSkillLevelEvent(character, skill, markLevel))
			markLevel += 1
		}
	}
}
