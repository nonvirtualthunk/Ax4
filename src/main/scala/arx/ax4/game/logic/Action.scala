package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.action.{AttackAction, GameAction, MoveAction}
import arx.ax4.game.event.ActionTaken
import arx.engine.world.World

object Action {
	def performAction(action : GameAction)(implicit world : World) : Boolean = {
		world.startEvent(ActionTaken(action))
		val res = action match {
			case MoveAction(character, path) =>
				Movement.moveCharacterOnPath(character, path)
			case a: AttackAction =>
//				Attacks.attack(a)
				???
				true
			case _ =>
				Noto.error(s"Unsupported action: $action")
				false
		}
		world.endEvent(ActionTaken(action))
		res
	}
}
