package arx.ax4.game.logic

import arx.application.Noto
import arx.ax4.game.action.{AttackAction, GameAction, GatherAction, MoveAction}
import arx.ax4.game.entities.Companions.Tile
import arx.ax4.game.entities.{GatherProspect, Tile, Tiles}
import arx.ax4.game.event.ActionTaken
import arx.engine.world.World

object ActionLogic {
	def performAction(action : GameAction)(implicit world : World) : Boolean = {
		implicit val view = world.view
		world.startEvent(ActionTaken(action))
		val res = action match {
			case MoveAction(character, path) =>
				MovementLogic.moveCharacterOnPath(character, path)
			case AttackAction(attacker, attack, attackFrom, targets, preMove, postMove) =>
				val entityTargets = targets match {
					case Left(entityTargets) => entityTargets
					case Right(hexTargets) => hexTargets.flatMap(hex => Tiles.tileAt(hex)(Tile).entities)
				}

				CombatLogic.attack(world, attacker, entityTargets.toList, attack)
				true
			case ga @ GatherAction(gatherer, target, resourceKey, method) =>
				GatherLogic.gather(GatherProspect(gatherer, target, resourceKey, method))
				true
			case _ =>
				Noto.error(s"Unsupported action: $action")
				false
		}
		world.endEvent(ActionTaken(action))
		res
	}
}
