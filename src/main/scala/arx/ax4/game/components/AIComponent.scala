package arx.ax4.game.components

import arx.ax4.game.entities.AllegianceData
import arx.ax4.game.event.TurnEvents.TurnStartedEvent
import arx.ax4.game.logic.{AllegianceLogic, IdentityLogic}
import arx.core.units.UnitOfTime
import arx.engine.entity.Entity
import arx.engine.game.components.GameComponent
import arx.engine.world.World

class AIComponent extends GameComponent {
	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		onGameEventEnd {
			case TurnStartedEvent(faction, _) if !AllegianceLogic.isPlayerFaction(faction) =>
				takeAITurn(faction)(world)
		}
	}

	def takeAITurn(faction: Entity)(implicit world: World) = {
		implicit val view = world.view

		for (entity <- AllegianceLogic.entitiesInFaction(faction)) {
			println(s"Taking turn for entity ${IdentityLogic.name(entity)}")
		}
	}
}
