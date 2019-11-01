package arx.ax4.game.logic

import arx.ax4.game.entities.{AllegianceData, CharacterInfo, FactionData}
import arx.engine.entity.Entity
import arx.engine.world.WorldView

object Allegiance {
	def isPlayerCharacter (character : Entity)(implicit view : WorldView) : Boolean = {
		character[AllegianceData].faction[FactionData].playerControlled
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
