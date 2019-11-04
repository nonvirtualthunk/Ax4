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
			case AttackAction(attacker, attack, targets, preMove, postMove) =>
				targets match {
					case Left(entityTargets) => CombatLogic.attack(world, attacker, entityTargets.toList, attack)
					case Right(hexTargets) => Noto.error("Hex targeting is not supported yet")
				}
				true
			case _ =>
				Noto.error(s"Unsupported action: $action")
				false
		}
		world.endEvent(ActionTaken(action))
		res
	}
}
