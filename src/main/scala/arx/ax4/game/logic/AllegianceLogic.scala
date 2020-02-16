package arx.ax4.game.logic

import arx.ax4.game.entities.{AllegianceData, CharacterInfo, FactionData}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object AllegianceLogic {
	def isPlayerCharacter (character : Entity)(implicit view : WorldView) : Boolean = {
		character[AllegianceData].faction[FactionData].playerControlled
	}

	def isPlayerFaction(faction : Entity)(implicit view : WorldView) : Boolean = {
		faction[FactionData].playerControlled
	}

	def entitiesInFaction(faction : Entity)(implicit view: WorldView) = {
		view.entitiesMatching[AllegianceData](ad => ad.faction == faction)
	}

	def areInSameFaction(characterA : Entity, characterB : Entity)(implicit view : WorldView) : Boolean = {
		val factionA = characterA.dataOpt[AllegianceData].map(_.faction)
		val factionB = characterB.dataOpt[AllegianceData].map(_.faction)
		factionA == factionB && factionA.isDefined
	}

	def areFriendly(characterA : Entity, characterB : Entity)(implicit view : WorldView) : Boolean = {
		if (!characterA.hasData[AllegianceData] || !characterB.hasData[AllegianceData]) {
			// if they do not have allegiance then they are not enemies or friends
			false
		} else {
			characterA[AllegianceData].faction == characterB[AllegianceData].faction
		}
	}

	def areEnemies(characterA : Entity, characterB : Entity)(implicit view : WorldView) : Boolean = {
		if (!characterA.hasData[AllegianceData] || !characterB.hasData[AllegianceData]) {
			// if they do not have allegiance then they are not enemies or friends
			false
		} else {
			!areFriendly(characterA, characterB)
		}
	}
}
