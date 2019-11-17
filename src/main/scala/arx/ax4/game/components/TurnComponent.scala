package arx.ax4.game.components

import arx.ax4.game.entities.AllegianceData
import arx.ax4.game.entities.Companions.CharacterInfo
import arx.ax4.game.event.AttackEvent
import arx.ax4.game.event.TurnEvents.{EntityTurnEndEvent, EntityTurnStartEvent, TurnEndedEvent, TurnStartedEvent}
import arx.core.units.UnitOfTime
import arx.engine.game.components.GameComponent
import arx.engine.world.World

class TurnComponent extends GameComponent {
	import arx.core.introspection.FieldOperations._

	override protected def onInitialize(world: World): Unit = {
		implicit val view = world.view
		onGameEventEnd {
			case TurnStartedEvent(faction, turnNumber) =>
				for (allegiant <- world.view.entitiesMatching[AllegianceData](ad => ad.faction == faction)) {
					world.startEvent(EntityTurnStartEvent(allegiant, turnNumber))
					world.modify(allegiant, CharacterInfo.actionPoints.recoverToFull())
					world.modify(allegiant, CharacterInfo.stamina recoverBy allegiant(CharacterInfo).staminaRecoveryRate)
					world.endEvent(EntityTurnStartEvent(allegiant, turnNumber))
				}
			case TurnEndedEvent(faction, turnNumber) =>
				for (allegiant <- world.view.entitiesMatching[AllegianceData](ad => ad.faction == faction)) {
					world.startEvent(EntityTurnEndEvent(allegiant, turnNumber))
					world.modify(allegiant, CharacterInfo.movePoints -> 0)
					world.modify(allegiant, CharacterInfo.actionPoints reduceTo 0)
					world.endEvent(EntityTurnEndEvent(allegiant, turnNumber))
				}
		}
	}

	override protected def onUpdate(world: World, dt: UnitOfTime): Unit = {

	}
}
