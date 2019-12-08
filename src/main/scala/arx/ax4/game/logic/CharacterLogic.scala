package arx.ax4.game.logic

import arx.ax4.game.action.GameActionIntent
import arx.ax4.game.entities.CharacterInfo
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.{ActiveIntentChanged, MovePointsGained}
import arx.engine.entity.Entity
import arx.engine.world.{World, WorldView}
import arx.core.introspection.FieldOperations._
import arx.core.math.Sext

object CharacterLogic {

	def curActionPoints(character : Entity)(implicit view : WorldView) = {
		character.dataOpt[CharacterInfo] match {
			case Some(ci) => ci.actionPoints.currentValue
			case None => 0
		}
	}

	def curMovePoints(character : Entity)(implicit view : WorldView) = {
		character.dataOpt[CharacterInfo].map(_.movePoints).getOrElse(Sext(0))
	}

	def useActionPoints(character : Entity, ap : Int)(implicit game : World) = {
		game.modify(character, CharacterInfo.actionPoints reduceBy ap)
	}

	def gainMovePoints(character : Entity, mp : Int)(implicit game : World) = {
		game.eventStmt(MovePointsGained(character, mp)) {
			game.modify(character, CharacterInfo.movePoints + mp)
		}
	}

	def setActiveIntent(entity : Entity, intent : GameActionIntent)(implicit game : World) = {
		game.startEvent(ActiveIntentChanged(entity, intent))
		game.modify(entity, CharacterInfo.activeIntent -> intent)
		game.endEvent(ActiveIntentChanged(entity, intent))
	}

}
