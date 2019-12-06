package arx.ax4.game.logic

import arx.ax4.game.action.GameActionIntent
import arx.ax4.game.entities.CharacterInfo
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.ActiveIntentChanged
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._

object CharacterLogic {

	def curActionPoints(character : Entity)(implicit view : WorldView) = {
		character.dataOpt[CharacterInfo] match {
			case Some(ci) => ci.actionPoints.currentValue
			case None => 0
		}
	}

	def useActionPoints(character : Entity, ap : Int)(implicit game : World) = {
		game.modify(character, CharacterInfo.actionPoints reduceBy ap)
	}

	def setActiveIntent(entity : Entity, intent : GameActionIntent)(implicit game : World) = {
		game.startEvent(ActiveIntentChanged(entity, intent))
		game.modify(entity, CharacterInfo.activeIntent -> intent)
		game.endEvent(ActiveIntentChanged(entity, intent))
	}

}
