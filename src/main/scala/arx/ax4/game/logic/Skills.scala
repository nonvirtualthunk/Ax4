package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.entities.{CharacterInfo, SkillsLibrary}
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.{GainSkillLevelEvent, GainSkillXPEvent}
import arx.engine.entity.{Entity, Taxon}
import arx.engine.world.{NestedKeyedModifier, NestedModifier, World, WorldView}

object Skills {
	import arx.core.introspection.FieldOperations._

	def xpForLevel(level : Int) = {
		10 + (level * (level - 1)) * 10

		// 10 + 0, 2, 6, 12, 20, 30,
		// 10 + 0, 20, 60, 120, 200, 300
		// 10, 30, 70, 130, 210, 310
	}

	def skillLevel(character : Entity, skill : Taxon)(implicit view : WorldView) = {
		character(CharacterInfo).skillLevels.getOrElse(skill, 0)
	}

	def gainSkillXP(character : Entity, skill : Taxon, amount : Int)(implicit world : World): Unit = {
		implicit val view = world.view

		val charInfo = character(CharacterInfo)
		val curXP = charInfo.skillXP.getOrElse(skill, 0)
		val newXP = curXP + amount

		val curLevel = skillLevel(character, skill)
		val newLevel = if (newXP >= xpForLevel(curLevel + 1)) {
			curLevel + 1
		} else {
			curLevel
		}

		world.startEvent(GainSkillXPEvent(character, skill, amount))
		world.modify(character, CharacterInfo.skillXP.put(skill, newXP))
		world.endEvent(GainSkillXPEvent(character, skill, amount))

		if (newLevel > curLevel) {
			world.startEvent(GainSkillLevelEvent(character, skill, newLevel))
			world.modify(character, CharacterInfo.skillLevels.put(skill, newLevel))

			SkillsLibrary(skill) match {
				case Some(skillInfo) =>
					skillInfo.skillLevel(newLevel) match {
						case Some(skillLevel) =>
							skillLevel.onLevelGained(world, character)
						case None =>
							Noto.warn(s"no actual skill level $newLevel for $skill")
					}

				case None =>
					Noto.warn(s"No information on skill $skill")
			}

			world.endEvent(GainSkillLevelEvent(character, skill, newLevel))
		}
	}
}
